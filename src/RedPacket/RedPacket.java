package RedPacket;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/*
* Author:CHL
* Date: 2020.05.23
*   最小金额 0.1元
 *  最大红包比列  0.6
 *  抢红包的个数和金额需要保证多线程访问更新的安全性
* */
public class RedPacket
{
    private final double MAX_RATE=0.6;
    private final double MIN_MONEY=0.1;
    AtomicStampedReference<BigDecimal> pari;
    //红包个数
    private Integer count;
    //红包金额
    private BigDecimal sum;
    //测试用,红包总金额是否有错
    static BigDecimal  sumMoney=new BigDecimal(0.0);
    public RedPacket(int count,double sum)
    {
        this.count=count;
         this.sum=new BigDecimal(sum);
        this.pari=new AtomicStampedReference<BigDecimal>(this.sum,count);
    }
    public BigDecimal getMoney()
    {
        while(true) {
            BigDecimal expectSum = this.pari.getReference();
            int expectCount = this.pari.getStamp();
            if (expectCount<1) {
                System.out.println("手速太慢，红包已经被抢完啦！");
                return new BigDecimal(0);
            } else if (expectCount== 1) {
                if (this.pari.compareAndSet(expectSum,new BigDecimal(0),expectCount,expectCount-1)) {
                    System.out.println("恭喜你抢到了最后一个红包" + expectSum+ "元");
                    return expectSum;
                }
            } else {
                BigDecimal rate = new BigDecimal(Math.random() * MAX_RATE).setScale(1,BigDecimal.ROUND_HALF_DOWN);
                BigDecimal result=expectSum.multiply(rate);
                if(result.doubleValue()<MIN_MONEY)
                {
                    result=new BigDecimal(0.1);
                }
                if (this.pari.compareAndSet(expectSum,expectSum.subtract(result),expectCount,expectCount-1)) {
                    System.out.println("恭喜你抢到了" + result+ "元");
                    return result;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int people=20;
        Lock  lock=new ReentrantLock();
        //进行30轮测试
        for(int j=0;j<30;j++) {
            //红包个数，红包金额
            RedPacket redPacket=new RedPacket(5,100.0);
            //用于一轮抢红包结束前，不进行新一轮的抢红包。（测试才需要）
            CountDownLatch latch=new CountDownLatch(people);
            System.out.println("抢红包开始啦===================================");
            for (int i = 1; i <= people; i++) {
                new Thread(() -> {
                    BigDecimal num = redPacket.getMoney();
                    lock.lock();
                    sumMoney=sumMoney.add(num);
                    lock.unlock();
                    latch.countDown();
                }).start();
            }
            latch.wait();
            System.out.println("抢红包结束啦=========总金额 "+sumMoney.doubleValue()+"=============================");
            sumMoney=sumMoney.subtract(sumMoney);
        }
    }
}

