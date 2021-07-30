## Reactive Programming: Reactive Streams

> 1. Processor
> 2. Publisher
> 3. Subscriber
> 4. Subscription
>
> 네 개의 인터페이스 (Interface)가 정해져 있다. 표준을 지킨다는 것은 이것을 구현하여 만들면 된다.
> 이 네개를 구현하여 Reactive Streams의 Simple한 Engine을 만들 수 있다.
>
> 각 각 Interface를 구현하는데는 Rule이 있다.
>
> * Publisher를 기존의 Observable이라 할 수 있다.
>> Publisher는 Iterable을 하나 가져와서 계속 Event/Data를 쏴준다
>
> * Subscriber를 기존의 Observer 라고 할 수 있다.
>> Publisher로부터 데이터를 받아온다.
>

 <I> Publisher 라는 것은 연속된 (순서를 갖는) 요소들을 한계가 없이 제공하고, 정보를
 요청한 Subscriber에게 받을 수 있도록 제공해야 한다.</I>

> Observable이 .addObserver를 사용했는데, Publisher는 .subscribe(Subsriber)를 호출한다.
>
> Publisher가 Subscriber에게 주는 정보는 Protocol을 따라야 한다.
> Publisher가 Subscriber에게 반드시 주어야 하는 Signal은 onSubscribe 메소드이다
> > Protocol:
>>
>> onSubscribe onNext* (onError | onComplete)?
>
> onSubscribe (Subscriber안에 메소드)를 호출 한 뒤, onNext를 0번 ~ N번까지 호출할 수 있고,
> Optional로 onError 또는 onComplete를 호출한다.
>

## Publisher & Subscriber 구현
* Publisher는 주는쪽이다. 주는쪽은 항상 누구한테 데이터를 줘야하는지 알아야한다.
* 근데 이건 구독 (Subscribe) 방식이다 !
* 즉 Subscriber가 나한테 줘 라고 Publisher에게서 subscribe (구독)을 해야한다.

> Publisher를 Subscriber가 구독 (subscribe), 이때 onSubscribe()는 무조건 호출 돼야한다.
>
> onSubscribe() 의 parameter는 subscription.
> Subscription Object안에는 request와 cancel이라는 메소드가 존재한다.
>
> Subscriber가 Publisher한테 너를 구독 (subscribe)할래 라고 subscribe()라는 메소드를 호출하면
> Publisher는 Subscripton이라는 Object를 만들어서, 다시 Subscriber에 onSubscribe() 메소드를 호출하고,
> Subscription을 통해 요청 (request)를 할 수 있다.
>
> 이때 request (요청)은 Publisher와 Subscriber 사이에 속도차이를 위한  **Back - Pressure** (역압) 기술을
> Reactive Streams에서는 Subscription을 통해서 처리한다.
>
> 만약 정보를 양에 상관없이 계속 받고싶다면 (Back pressure 없이), Subscription Object안에 request
> 메소드 parameter에 long n , 값을 크게 넣어주면 된다.
> 최초 request는 onSubscribe에서 한다.
>
> 만약 최초 request에 1을 넘겨서 1개만 받았는데 이후 남은 데이터를 더 받고싶다면?
> onNext()에서 처리한다

더 나은 Observer Pattern 이라는게 Reactive Programming의 한 축이라면,
다른 축은 Scehduler !

> Scheduler는 비동기적으로, 또는 동시에 병렬적으로 작업을 수행.
>
> Reactive Stream은 결국은 동시성을 갖고 간결하게 만들기 위해 사용된다.