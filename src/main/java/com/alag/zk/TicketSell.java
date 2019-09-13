package com.alag.zk;

import com.alag.zk.lock.ZkLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;

@Slf4j
@Component
public class TicketSell {

    private Integer count = 100;

    public void start() {
        TicketThread ticketThread = new TicketThread();

        Thread t1 = new Thread(ticketThread, "t1");
        Thread t2 = new Thread(ticketThread, "t2");
        Thread t3 = new Thread(ticketThread, "t3");
        Thread t4 = new Thread(ticketThread, "t4");

        t1.start();
        t2.start();
        t3.start();
        t4.start();
    }

    public class TicketThread implements Runnable {
        @Override
        public void run() {
            while (count > 0) {
                Lock lock = new ZkLock();
                lock.lock();
                try {
                    if (count > 0) {
                        System.out.println(Thread.currentThread().getName() + "----抢到票编号:" + count--);
                    }
                } catch (Exception e) {
                    log.info(e.getMessage());
                }finally {
                    lock.unlock();
                }
            }
        }
    }
}
