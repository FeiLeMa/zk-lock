package com.alag.zk.lock;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
public class ZkLock implements Lock {
    private static final String LOCK_PATH = "/lock";
    private static final String ZOOKEEPER_IP_PORT = "192.168.0.102:2181";
    private ZkClient zkClient = new ZkClient(ZOOKEEPER_IP_PORT, 1000, 1000, new SerializableSerializer());
    private CountDownLatch countDownLatch;

    private String beforePath;
    private String currentPath;

//    判断有没有Lock没有就创建


    public ZkLock() {
        if (!zkClient.exists(LOCK_PATH)) {
            zkClient.createPersistent(LOCK_PATH);
        }
    }
//    阻塞式加锁
    @Override
    public void lock() {
        if (tryLock()) {
            return;
        }
        waitForLock();
        lock();
    }

    private void waitForLock() {
        IZkDataListener iZkDataListener = new IZkDataListener() {
            @Override
            public void handleDataDeleted(String s) throws Exception {
                log.info("已经捕获到节点被删除事件");
                if (countDownLatch != null) {
                    countDownLatch.countDown();
                }
            }

            @Override
            public void handleDataChange(String s, Object o) throws Exception {}
        };

        this.zkClient.subscribeDataChanges(beforePath,iZkDataListener);
        if (this.zkClient.exists(beforePath)) {
            countDownLatch = new CountDownLatch(1);
            try {
//                线程被阻塞之后当1-1==0才会放行，也就是节点被删除
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.zkClient.unsubscribeDataChanges(beforePath,iZkDataListener);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }
//    非阻塞式加锁
    @Override
    public boolean tryLock() {
//        判断有没有加锁
        if (currentPath == null || currentPath.length() <= 0) {
//            没有就创建一个临时顺序节点,会获得一个自增长的字符串：0000000118
            currentPath = zkClient.createEphemeralSequential(LOCK_PATH + "/", "lock");
            log.info("当前节点:{}",currentPath);
        }
        List<String> children = this.zkClient.getChildren(LOCK_PATH);
        Collections.sort(children);

//        判断自己是不是第一个节点
        if (currentPath.equals(LOCK_PATH + "/" + children.get(0))) {
            return true;
        } else {
//            去掉前面的6位得到当前节点在List集合中的值，找到他在集合中的index
            int wz = Collections.binarySearch(children, currentPath.substring(6));
            beforePath = LOCK_PATH + "/" + children.get(wz - 1);
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        this.zkClient.delete(currentPath);
        this.zkClient.close();
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
