## Java Spring WebFlux: Reactive Streams [Observable]
> Reactive Programming을 이해하자.

### Reactive Streams
> 이게 표준이다. 주로 Java or JVM 기반의 회사에서 협력하여 적당한 레벨의 표준 스팩을 정하여 만든 것.
>
> 구현 가이드 등이 규정돼 있다.
> Java 9에는 기본으로 API가 들어가있다.
> 핵심 Interface와 규칙이 존재한다.

#### Iterable
> List<Integer> list = Arrays.asList(1, 2, 3, 4, 5); // 이런 방식이 존재
>
> 이것을 Iterable로. List는 Collection을 상속하고, Collection 안에는 Iterable 인터페이스를 상속한다.
>
> Iterable<Integer> iter = Arrays.asList(1, 2, 3, 4, 5);
>
> Iterable 안에 Iterator --> 순회 할때는 Iterator를 이용한다. 즉 도구이다. Iterable이 갖고있는 원소를 순회할때 사용


#### Iterable <---> Observable (Duality)
> Iterable의 쌍대성 (이중성) ? Duality..?은 Observable이다.
>
> 궁극적인 기능은 똑같은데 방향이 다르다.

#### Observable
> * Iterable은 Pull (풀링) 방식이다. 즉 내가 그 다음꺼가 필요할때 땡겨서 가져오는 방식.
>
>
> * Observable은 Push 개념이다. 즉 어떤 데이터, event를 갖고있는 Source 쪽에서 데이터를 밀어준다.
>

> Observable이란 Source (Event Source)라 생각할 수 있다.
>
> Source -> Event/Data -> Observer
>
> 우리가 Source에다가 Observable을 만들고, Observer를 만들어서 Observable에게 Observer를 등록하는것
> Observable은 그때부터 새로운 정보가 발생 할 때 마다 Observer에게 넘겨준다.
>
> Observer는 여러개가 될 수 있다.
>
> Reactive Streams 에서 Observable이 Publisher,
> Observer가 subscriber 라고 할 수 있다.
>
> Publisher (Observable)이 Data/Event (예를 들어 DB에서 데이터를 가져온다 등)
> 만들어 내면, 이걸 관심있어하는 모든 Subscriber (Observer)한테 한꺼번에 Broadcast (multicast)를 할 수 있다.
>
> DATA method(void) <---> void method(Data)
>
> 기능은 동일

### 기존 Observable (Observe pattern)의 문제점
> 1. 종료 (Complete) 시점이 존재하지 않는다.
>
> - Observable이 데이터를 다 준 다음 '끝' 이라는 개념이 없다.
    > Complete 을 어떻게 시킬 것이냐? 마지막 데이터에 Complete을 뜻하는 특정한 값을 같이 보낸다?
> - 물론 끝나지 않는 Event가 존재할 수 있다.
> - 하지만 종료시점이 필요한 경우, 데이터를 다 보냈을 때 완료를 해줄 수가 없다. Notify 밖에 없음..
>
> 2. Error
> - 만약 Exception이 발생했다면?
> - Observable과 Observe를 비동기적으로 별도의 쓰레드에서 동작하도록 정의했을 때는
    > 발생된 예외를 전파하는 방식, 받은 예외를 처리하는 방식, 재시도 등에 대한 것이
    > 이 패턴에는 존재하지 않는다.
>
> 이 두가지가 빠져서 만들어진 패턴은 심각한 한계라 보고
>
> 이 두 가지를 추가하여 확장한 Observer Pattern이 Reactive Programming의
> 커다란 3 가지 축 중에 하나이다.