package com.example.springwebflux.ReactiveStudy2;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

// Publisher & Subscriber
public class PubSub {
    public static void main(String[] args) {
        // Publisher <- Observable
        // Subscriber <- Observer

        Iterable<Integer> itr = Arrays.asList(1, 2, 3, 4, 5);

        /**
         * Publisher 를 구현
         *
         * Publisher는 주는쪽이다. 주는쪽은 항상 누구한테 데이터를 줘야하는지 알아야한다.
         * 근데 이건 구독 (Subscribe) 방식이다 !
         * 즉 Subscriber가 나한테 줘 라고 Publisher에게서 subscribe (구독)을 해야한다.
         */
        Publisher p = new Publisher() { // 인터페이스가 한 개니까 lambda로 만들수 있지만.. 그냥 우선 두자
            @Override
            public void subscribe(Subscriber subscriber) { // Publisher를 구독해서 값을 받아오기 위해  ! Subscriber가 호출한다
                Iterator<Integer> it = itr.iterator();

                subscriber.onSubscribe(new Subscription() { // 필수로 호출해야한다. parameter type은 Subscrition
                    @Override
                    public void request(long n) {
                        // request에서는 주고싶은걸 하나씩 주면된다 여기서는 iterable 안에있는 데이터

                        while (n-- > 0) { // 넘어온 n만큼만 수행
                            if (it.hasNext()) { // 아직 줄 데이터가 있다면
                                subscriber.onNext(it.next()); // subscriber에게 요청한 데이터 it를 넘긴다
                            } else { // 더이상 줄 데이터가 없다면
                                subscriber.onComplete();
                                break;
                            }
                        }
                    }

                    /**
                     * onSubscribe
                     * onNext 1
                     * onNext 2
                     * onNext 3
                     * onNext 4
                     * onNext 5
                     * onComplete
                     */

                    @Override
                    public void cancel() {

                    }
                });
            }
        };

        /**
         * Subscriber는 Methods가 네 개
         * onSubscriber는 필수, onNext는 0~N개 옵션, onError 또는 onComplete은 둘 중하나 옵션
         */
        Subscriber<Integer> s = new Subscriber<Integer>() {
            Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                System.out.println("onSubscribe");
                this.subscription = subscription; // 우선 subscription ㅓ장
                this.subscription.request(1); // 몽땅 받고싶다? Long.MAX_VALUE();
            }


            int bufferSize = 2; // back-pressure에 사용될 buffer size

            @Override
            public void onNext(Integer item) {
                /**
                 * Observer Pattern에 update와 비슷한것.
                 * Publisher가 Data를 주면 onNext가 받는다.
                 * 즉 다음꺼를 주었으니까 처리해 라는 역할의 메소드이다.
                 */

                System.out.println("onNext " + item);
                this.subscription.request(1); // 최초 request (onSubscribe에서)때 1개만 받고, 이후 추가적으로 받기위해~
                // 만약 최초 request 때 2 개? 이후 onNext에서 1개만 호출했다면?
                // 이것보단 더 효율적으로, 보통 최대 버퍼의 절반을 유지하며 최대한 많이 받는 등 더 효율적으로 한다.
                // 하지만 우리가 고민 할 필요가 없다. 왜냐? Scheduler..
                /*
                if (--bufferSize <= 0) {
                    bufferSize = 2;
                    this.subscription.request(2);
                }
                 */

            }

            @Override
            public void onError(Throwable throwable) {
                /**
                 * Exception 발생 말고, Exception에 해당하는 Object를 만들어서 onError를 타고
                 * 나한테 넘기라는 의미
                 */
                System.out.println("onError");
            }

            @Override
            public void onComplete() {
                /**
                 * Publisher가 더이상 줄 데이터가 없다는것을 명시
                 */

                System.out.println("onComplete");
            }

        };

        p.subscribe(s); // Publisher 메소드에 subscribe에 subscriber를 전달.

    }
}