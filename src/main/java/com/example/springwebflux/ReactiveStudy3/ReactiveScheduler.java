package com.example.springwebflux.ReactiveStudy3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReactiveScheduler {
    public static void main(String[] args) {
        /*
        Publisher<Integer> pub = sub -> {
            sub.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    sub.onNext(1);
                    sub.onNext(2);
                    sub.onNext(3);
                    sub.onNext(4);
                    sub.onNext(5);
                    sub.onComplete();
                }
                @Override
                public void cancel() {
                }
            });
        };
         */
/*
        pub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                log.debug("onSubscribe");
                s.request(Long.MAX_VALUE);
            }
            @Override
            public void onNext(Integer integer) {
                log.debug("onNext: {}", integer);
            }
            @Override
            public void onError(Throwable t) {
                log.debug("onError: {}", t);
            }
            @Override
            public void onComplete() {
                log.debug("onComplete");
            }
        });
 */

        /**
         * subscribeOn 에 실행되는 Publisher
         */
        /*
        Publisher<Integer> subOnPub = sub -> {
            ExecutorService es = Executors.newSingleThreadExecutor(); // 별도의 Thread를 생성.
            es.execute(() -> pub.subscribe(sub));
        };
         */
        /*
        subOnPub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                log.debug("onSubscribe");
                s.request(Long.MAX_VALUE);
            }
            @Override
            public void onNext(Integer integer) {
                log.debug("onNext: {}", integer);
            }
            @Override
            public void onError(Throwable t) {
                log.debug("onError: {}", t);
            }
            @Override
            public void onComplete() {
                log.debug("onComplete");
            }
        });
         */

        /**
         * publishOn
         * 실제 Data를 처리하는 (전달되는) onNext, onError, onComplete을 별개의 Thread에서 수행하도록.
         *
         * 즉 그 중간에서 중개를 해주는 별도의 Subscriber를 하나를 더 만든다.
         */
        /*
        Publisher<Integer> pubOnPub = sub -> {
            pub.subscribe(new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription s) {
                    sub.onSubscribe(s);
                }
                ExecutorService es = Executors.newSingleThreadExecutor();
                @Override
                public void onNext(Integer integer) {
                    es.execute(() -> sub.onNext(integer));
                }
                @Override
                public void onError(Throwable t) {
                    es.execute(() -> sub.onError(t));
                }
                @Override
                public void onComplete() {
                    es.execute(sub::onComplete);
                }
            });
        };
        pubOnPub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                log.debug("onSubscribe");
                s.request(Long.MAX_VALUE);
            }
            @Override
            public void onNext(Integer integer) {
                log.debug("onNext: {}", integer);
            }
            @Override
            public void onError(Throwable t) {
                log.debug("onError: {}", t);
            }
            @Override
            public void onComplete() {
                log.debug("onComplete");
            }
        });
        */

        /**
         * publishOn, subscribeOn 두 가지를 동시에 적용시키기
         */

        Publisher<Integer> pub = sub -> {
            sub.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    log.debug("request()");
                    sub.onNext(1);
                    sub.onNext(2);
                    sub.onNext(3);
                    sub.onNext(4);
                    sub.onNext(5);
                    sub.onComplete();
                }

                @Override
                public void cancel() {

                }
            });
        };

        Publisher<Integer> subOnPub = sub -> {
            ExecutorService es = Executors.newSingleThreadExecutor(new CustomizableThreadFactory() {

                @Override
                public String getThreadNamePrefix() {
                    return "subOn-";
                }
            }); // 별도의 Thread를 생성 이름 subOn.
            es.execute(() -> pub.subscribe(sub));
        };

        Publisher<Integer> pubOnPub = sub -> {
            subOnPub.subscribe(new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription s) {
                    sub.onSubscribe(s);
                }

                ExecutorService es = Executors.newSingleThreadExecutor(new CustomizableThreadFactory() {

                    @Override
                    public String getThreadNamePrefix() {
                        return "pubOn-";
                    }
                });

                /**
                 * 별개의 Thread를 통해서 작동시키고싶다
                 */
                @Override
                public void onNext(Integer integer) {
                    es.execute(() -> sub.onNext(integer));
                }

                @Override
                public void onError(Throwable t) {
                    es.execute(() -> sub.onError(t));
                    es.shutdown();
                }

                @Override
                public void onComplete() {
                    es.execute(sub::onComplete);
                    es.shutdown();
                }
            });
        };

        pubOnPub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                log.debug("onSubscribe");
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Integer integer) {
                log.debug("onNext: {}", integer);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("onError: {}", t);
            }

            @Override
            public void onComplete() {
                log.debug("onComplete");
            }
        });
    }
}
