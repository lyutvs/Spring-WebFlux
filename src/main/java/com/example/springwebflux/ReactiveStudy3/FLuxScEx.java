package com.example.springwebflux.ReactiveStudy3;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * React library를 이용한 방법.
 * Reactive Streams를 어떻게 만들어 놨는가
 */
@Slf4j
public class FLuxScEx {
    public static void main(String[] args) throws InterruptedException {
        /**
         * Flux: Publisher API
         */
        /*
        Flux.range(1, 10)
            .publishOn(Schedulers.newSingle("pub"))
            .log()
            .subscribeOn(Schedulers.newSingle("sub"))
            .subscribe(System.out::println);
        System.out.println("exit");
         */

        /**
         * Thread를 별도로 할당하지 않았음애도, 자동으로 별도의 Thread로 동작하는 것.
         * 1. interval: 어떠한 주기를 가지고 숫자값을 0~ 무한대로 쏴주는 것 (강제 종료 전까지)
         */

        Flux.interval(Duration.ofMillis(200))
                .take(10)
                .subscribe(s -> log.debug("onNext:{}", s));

        log.debug("Exit");

        TimeUnit.SECONDS.sleep(10);
    }
}