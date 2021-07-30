package com.example.springwebflux.ReactiveStudy1;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("Deprecation")
public class ReactiveProgramming {

    public static void main(String[] args) {

        System.out.println("<------------Iterable------------>\n");
        /**
         * Reactive Programming, ReactiveX
         * FRP (Functional Reactive Programming): 이미 80년대부터 나온것들... Event 방식의 programming
         * <p>
         * Reactive란 외부에 어떤 event가 발생하면 거기에 대응하는 방식으로 동작.
         * <p>
         * Duality:
         * Observer Pattern (Design Pattern): Listener.. 이러한 Event를 핸들링하는걸 만들어서 작동
         * Reactive Streams - 표준
         */

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5); // 이런 방식이 존재
        // 이것을 Iterable로. List는 Collection을 상속하고, Collection 안에는 Iterable 인터페이스를 상속한다.
        Iterable<Integer> iter = Arrays.asList(1, 2, 3, 4, 5);
        // Iterable 안에 Iterator --> 순회 할때는 Iterator를 이용한다. 즉 도구이다. Iterable이 갖고있는 원소를 순회할때 사용

        // Iterable을 구현 (iterator를 사용해보자 순회하면서~)
        Iterable<Integer> iterable = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return null;
            }
        };
        // 이러한 Interface에 하나의 메소드만을 포함하고 있다면 Lambda 방식으로 작성할 수 있다. 굳이 익명 클래스로 만들 필요없음
        Iterable<Integer> integers = () -> {
            return null;
        };

        // 이제 Iterable 인터페이스를 구현하여 Lambda 식으로 Iterator 도구를 사용하여 순회하면서 원소를 1~5까지 순회하는 Object를 만들자
        Iterable<Integer> iterImplements = () ->
                new Iterator<Integer>() { // 구현체이다
                    int i = 0;
                    final static int MAX = 5;

                    @Override
                    public boolean hasNext() {
                        return i < MAX;
                    }

                    @Override
                    public Integer next() {
                        return ++i;
                    }
                };

        for (Integer i : iterImplements) {
            System.out.println(i);
        }

        // Java 5 이전에 for문을 어떻게 썻느냐?
        for (Iterator<Integer> it = iterImplements.iterator(); it.hasNext(); ) {
            System.out.println(it.next());
        }

        System.out.println("\n\n<------------Observable------------>\n");

        // Java 안에 Observable 인터페이스가 존재를 한다. Java 1.0 시절부터 존재했음
        Observer ob = new Observer() {
            @Override
            public void update(Observable o, Object arg) { // 현재 Observable에 등록된 모든 Observer 한태 보내준다. Subscriber 같은..
                System.out.println(Thread.currentThread().getName() + " " + arg);
            }
        };

        IntObervable io = new IntObervable(); // Integer Observable object를 만든다.
        io.addObserver(ob); // Observer ob (object)를 addObserver로 등록. 이때부터 IntObservable이 던지는걸 다 ob가 받는다

        /*
        이벤트가 언제 일어날지 모르는데 메인 메소드에서 Thread가 블럭 당하지 않게 별도 Thread에서 동작하도록 할 수 있다.
        그러고 이벤트 발생시 동작하도록 할 수 있다.
         */
        ExecutorService es = Executors.newSingleThreadExecutor(); // 별도의 작업 Thread를 정의
        //io.run(); // IntObservable이 던진 1~10을 Observer ob 가 받아 출력했다.
        es.execute(io); // 별도의 Thread에서 동작하도록
        System.out.println(Thread.currentThread().getName() + " " + "EXIT");
        es.shutdown();
    }

    static class IntObervable extends Observable implements Runnable { // Runnerble을 넣어서 비 동기적으로 동작시키기 위해

        @Override
        public void run() {
            for (int i = 1; i <= 10; i++) {
                setChanged(); // 던지기 전에 새로운 변화가 생겼다는 것을 setChange() 메소드로 호출
                notifyObservers(i); // 던져준다. push
                // int i = it.next() // pull
            }
        }
    }
}
