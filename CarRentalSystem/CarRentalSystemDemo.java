package CarRentalSystem;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/* ===========================
   ENUMS
   =========================== */

enum CarType {
    SEDAN, SUV, HATCHBACK
}

enum ReservationStatus {
    CREATED, CANCELLED
}

/* ===========================
   CAR ENTITY
   =========================== */

class Car {
    private final String id;
    private final String make;
    private final String model;
    private final CarType carType;
    private final double pricePerDay;
    private boolean available = true;

    public Car(String id, String make, String model, CarType carType, double pricePerDay) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.carType = carType;
        this.pricePerDay = pricePerDay;
    }

    public String getId() { return id; }
    public CarType getCarType() { return carType; }
    public double getPricePerDay() { return pricePerDay; }

    public synchronized boolean isAvailable() {
        return available;
    }

    public synchronized void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return make + " " + model + " (" + carType + ")";
    }
}

/* ===========================
   CUSTOMER
   =========================== */

class Customer {
    private final String name;
    private final String licenseNumber;

    public Customer(String name, String licenseNumber) {
        this.name = name;
        this.licenseNumber = licenseNumber;
    }

    public String getName() { return name; }
}

/* ===========================
   RESERVATION
   =========================== */

class Reservation {
    private final String id;
    private final Car car;
    private final Customer customer;
    private final LocalDate start;
    private final LocalDate end;
    private final double totalPrice;
    private ReservationStatus status;

    public Reservation(Car car, Customer customer, LocalDate start, LocalDate end) {
        this.id = UUID.randomUUID().toString();
        this.car = car;
        this.customer = customer;
        this.start = start;
        this.end = end;
        this.totalPrice =
                ChronoUnit.DAYS.between(start, end) * car.getPricePerDay();
        this.status = ReservationStatus.CREATED;
    }

    public double getTotalPrice() { return totalPrice; }

    public void cancel() {
        status = ReservationStatus.CANCELLED;
        car.setAvailable(true);
    }
}

/* ===========================
   PAYMENT STRATEGY
   =========================== */

interface PaymentStrategy {
    void pay(double amount);
}

class CreditCardPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + " using Credit Card");
    }
}

class UpiPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + " using UPI");
    }
}

/* ===========================
   SEARCH CRITERIA (Predicate-based)
   =========================== */

class SearchCriteria {
    private Predicate<Car> predicate = car -> true;

    public void byCarType(CarType type) {
        predicate = predicate.and(car -> car.getCarType() == type);
    }

    public void byMaxPrice(double maxPrice) {
        predicate = predicate.and(car -> car.getPricePerDay() <= maxPrice);
    }

    public Predicate<Car> build() {
        return predicate;
    }
}

/* ===========================
   SINGLETON CAR RENTAL SYSTEM
   =========================== */

class CarRentalSystem {

    private static volatile CarRentalSystem instance;

    private final Map<String, Car> cars = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    private CarRentalSystem() {}

    public static CarRentalSystem getInstance() {
        if (instance == null) {
            synchronized (CarRentalSystem.class) {
                if (instance == null) {
                    instance = new CarRentalSystem();
                }
            }
        }
        return instance;
    }

    /* ---------- Inventory ---------- */

    public void addCar(Car car) {
        cars.put(car.getId(), car);
    }

    /* ---------- Search ---------- */

    public List<Car> searchCars(SearchCriteria criteria) {
        List<Car> result = new ArrayList<>();
        for (Car car : cars.values()) {
            if (criteria.build().test(car) && car.isAvailable()) {
                result.add(car);
            }
        }
        return result;
    }

    /* ---------- Reservation ---------- */

    public Future<Reservation> reserveCar(
            String carId,
            Customer customer,
            LocalDate start,
            LocalDate end,
            PaymentStrategy paymentStrategy) {

        return executor.submit(() -> {

            Car car = cars.get(carId);

            synchronized (car) {
                if (!car.isAvailable()) {
                    throw new RuntimeException("Car not available");
                }
                car.setAvailable(false);
            }

            Reservation reservation =
                    new Reservation(car, customer, start, end);

            paymentStrategy.pay(reservation.getTotalPrice());
            return reservation;
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}

/* ===========================
   MAIN CLASS
   =========================== */

public class CarRentalSystemDemo {

    public static void main(String[] args) throws Exception {

        CarRentalSystem system = CarRentalSystem.getInstance();

        system.addCar(new Car("1", "Toyota", "Camry", CarType.SEDAN, 1000));
        system.addCar(new Car("2", "Honda", "CRV", CarType.SUV, 1500));

        SearchCriteria criteria = new SearchCriteria();
        criteria.byCarType(CarType.SEDAN);
        criteria.byMaxPrice(1200);

        List<Car> cars = system.searchCars(criteria);
        System.out.println("Available cars: " + cars);

        Customer customer = new Customer("Aishwarya", "DL12345");

        Future<Reservation> reservationFuture =
                system.reserveCar(
                        "1",
                        customer,
                        LocalDate.now(),
                        LocalDate.now().plusDays(3),
                        new CreditCardPayment()
                );

        Reservation reservation = reservationFuture.get();
        System.out.println("Reservation successful, amount paid.");

        system.shutdown();
    }
}
