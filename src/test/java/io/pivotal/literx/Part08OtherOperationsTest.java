package io.pivotal.literx;

import io.pivotal.literx.domain.User;
import io.pivotal.literx.repository.ReactiveRepository;
import io.pivotal.literx.repository.ReactiveUserRepository;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;
import reactor.util.function.Tuple2;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Learn how to use various other operators.
 *
 * @author Sebastien Deleuze
 */
public class Part08OtherOperationsTest {

    Part08OtherOperations workshop = new Part08OtherOperations();

    final static User MARIE = new User("mschrader", "Marie", "Schrader");
    final static User MIKE = new User("mehrmantraut", "Mike", "Ehrmantraut");

//========================================================================================

    @Test
    public void zipFirstNameAndLastName() {
        Flux<String> usernameFlux = Flux.just(User.SKYLER.getUsername(), User.JESSE.getUsername(), User.WALTER.getUsername(), User.SAUL.getUsername());
        Flux<String> firstnameFlux = Flux.just(User.SKYLER.getFirstname(), User.JESSE.getFirstname(), User.WALTER.getFirstname(), User.SAUL.getFirstname());
        Flux<String> lastnameFlux = Flux.just(User.SKYLER.getLastname(), User.JESSE.getLastname(), User.WALTER.getLastname(), User.SAUL.getLastname());
        Flux<User> userFlux = workshop.userFluxFromStringFlux(usernameFlux, firstnameFlux, lastnameFlux);
        StepVerifier.create(userFlux)
                .expectNext(User.SKYLER, User.JESSE, User.WALTER, User.SAUL)
                .verifyComplete();
    }

    static class Sub implements Subscriber<String> {
        Subscription sub;

        @Override
        public void onSubscribe(Subscription s) {
            System.out.println("onSubscribe");
            this.sub = s;

        }

        public void req(int n) {
            sub.request(n);
        }

        @Override
        public void onNext(String s) {
            System.out.println(s);
        }

        @Override
        public void onError(Throwable t) {

            System.out.println("onError");
            t.printStackTrace();
        }

        @Override
        public void onComplete() {
            System.out.println("onComplete");
        }
    }

    @Test
    public void zip() throws InterruptedException {
        ReactiveUserRepository repository1 = new ReactiveUserRepository( new User("A"), new User("B"), new User("C"), new User("D"), new User("E"), new User("F"));
        ReactiveUserRepository repository2 = new ReactiveUserRepository( new User("1"), new User("2"), new User("3"), new User("4"));
        Flux<User> user1 = repository1.findAll();
        Flux<User> user2 = repository2.findAll();
        Sub sub = new Sub();
        Flux.zip(obs -> {
            System.out.println(Thread.currentThread().getName());
            User u1 = (User) obs[0];
            User u2 = (User) obs[1];
            return u1.getName() + " " + u2.getName();
        }, user1, user2)
                .subscribe(sub);
        System.out.println(Thread.currentThread().getName());
        sub.req(4);
        Thread.sleep(1000);
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newCachedThreadPool();
        pool.execute(()->{
            System.out.println("11111111");
        });
        pool.execute(()->{
            System.out.println("22222222");
        });
        while (true){
            System.out.println("33333333");
        }
    }
//========================================================================================

    @Test
    public void fastestMono() {
        ReactiveRepository<User> repository = new ReactiveUserRepository(MARIE);
        ReactiveRepository<User> repositoryWithDelay = new ReactiveUserRepository(250, MIKE);
        Mono<User> mono = workshop.useFastestMono(repository.findFirst(), repositoryWithDelay.findFirst());
        StepVerifier.create(mono)
                .expectNext(MARIE)
                .verifyComplete();

        repository = new ReactiveUserRepository(250, MARIE);
        repositoryWithDelay = new ReactiveUserRepository(MIKE);
        mono = workshop.useFastestMono(repository.findFirst(), repositoryWithDelay.findFirst());
        StepVerifier.create(mono)
                .expectNext(MIKE)
                .verifyComplete();
    }

//========================================================================================

    @Test
    public void fastestFlux() {
        ReactiveRepository<User> repository = new ReactiveUserRepository(MARIE, MIKE);
        ReactiveRepository<User> repositoryWithDelay = new ReactiveUserRepository(250);
        Flux<User> flux = workshop.useFastestFlux(repository.findAll(), repositoryWithDelay.findAll());
        StepVerifier.create(flux)
                .expectNext(MARIE, MIKE)
                .verifyComplete();

        repository = new ReactiveUserRepository(250, MARIE, MIKE);
        repositoryWithDelay = new ReactiveUserRepository();
        flux = workshop.useFastestFlux(repository.findAll(), repositoryWithDelay.findAll());
        StepVerifier.create(flux)
                .expectNext(User.SKYLER, User.JESSE, User.WALTER, User.SAUL)
                .verifyComplete();
    }

//========================================================================================

    @Test
    public void complete() {
        ReactiveRepository<User> repository = new ReactiveUserRepository();
        PublisherProbe<User> probe = PublisherProbe.of(repository.findAll());
        Mono<Void> completion = workshop.fluxCompletion(probe.flux());
        StepVerifier.create(completion)
                .verifyComplete();
        probe.assertWasRequested();
    }

//========================================================================================

    @Test
    public void nullHandling() {
        Mono<User> mono = workshop.nullAwareUserToMono(User.SKYLER);
        StepVerifier.create(mono)
                .expectNext(User.SKYLER)
                .verifyComplete();
        mono = workshop.nullAwareUserToMono(null);
        StepVerifier.create(mono)
                .verifyComplete();
    }

//========================================================================================

    @Test
    public void emptyHandling() {
        Mono<User> mono = workshop.emptyToSkyler(Mono.just(User.WALTER));
        StepVerifier.create(mono)
                .expectNext(User.WALTER)
                .verifyComplete();
        mono = workshop.emptyToSkyler(Mono.empty());
        StepVerifier.create(mono)
                .expectNext(User.SKYLER)
                .verifyComplete();
    }

//========================================================================================

    @Test
    public void collect() {
        ReactiveRepository<User> repository = new ReactiveUserRepository();
        Mono<List<User>> collection = workshop.fluxCollection(repository.findAll());
        StepVerifier.create(collection)
                .expectNext(Arrays.asList(User.SKYLER, User.JESSE, User.WALTER, User.SAUL))
                .verifyComplete();
    }

}
