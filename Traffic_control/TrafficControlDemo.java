package Traffic_control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TrafficControlDemo {
    public static void main(String[] args) {
        TrafficController system = TrafficController.getInstance();

        system.addIntersection(1, 500, 200);
        system.addIntersection(2, 700, 150);

        system.startSystem();
        try {
            TimeUnit.SECONDS.sleep(5);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        system.stopSystem();
    }

}

class TrafficController {
    private static final TrafficController trafficController = new TrafficController();
    private final List<IntersectionController> intersections = new ArrayList<>();
    private ExecutorService executorService;

    TrafficController() {

    }

    static TrafficController getInstance() {
        return trafficController;
    }

    public void addIntersection(int intersetionId, int greenDuration, int yellowDuration) {
        IntersectionController intersection = new IntersectionController.Builder(intersetionId)
                .withDurations(greenDuration, yellowDuration)
                .addObserver(new CentralMonitor())
                .build();
        intersections.add(intersection);
    }

    public void startSystem() {
        if (intersections.isEmpty()) {
            System.out.println("No intersections to manage. system not starting.");
            return;
        }
        System.out.println("Starting Traffic Control System...");
        executorService = Executors.newFixedThreadPool(intersections.size());
        intersections.forEach(executorService::submit);
    }

    public void stopSystem() {
        System.out.println("\n shuttig down Traffic Control System...");
        intersections.forEach(IntersectionController::stop);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();

        }
        System.out.println("All intersections stopped. System shut down.");
    }
}

class IntersectionController implements Runnable {
    int id;
    private final Map<Direction, TrafficLight> trafficLights;
    private IntersectionState currentState;
    private final long greenDuration;
    private final long yellowDuration;
    private volatile boolean running = true;

    private IntersectionController(int id, Map<Direction, TrafficLight> trafficLights, long greenDuration,
            long yellowDuration) {
        this.id = id;
        this.trafficLights = trafficLights;
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        // Initial state for the intersection
        this.currentState = new NorthSouthGreenState();
    }

    public int getId() {
        return id;
    }

    public long getGreenDuration() {
        return greenDuration;
    }

    public long getYellowDuration() {
        return yellowDuration;
    }

    public TrafficLight getLight(Direction direction) {
        return trafficLights.get(direction);
    }

    public void setState(IntersectionState state) {
        this.currentState = state;
    }

    public void stop() {
        this.running = false;
         Thread.currentThread().interrupt();
    }

    @Override
    public void run() {
        while (running) {
            try {
                currentState.handle(this);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Intersection" + id + " was interupted.");
                running = false;
            }

        }
    }

    public static final class Builder {
        private final int id;
        private long greenDuration = 5000;
        private long yellowDuration = 2000;
        private final List<TrafficObserver> observers = new ArrayList<>();

        public Builder(int id) {
            this.id = id;
        }

        public Builder withDurations(long green, long yellow) {
            this.greenDuration = green;
            this.yellowDuration = yellow;
            return this;
        }

        public Builder addObserver(TrafficObserver observer) {
            this.observers.add(observer);
            return this;
        }

        public IntersectionController build() {
            Map<Direction, TrafficLight> lights = new HashMap<>();
            for (Direction dir : Direction.values()) {
                TrafficLight light = new TrafficLight(id, dir);
                observers.forEach(light::addObserver);
                lights.put(dir, light);
            }
            return new IntersectionController(id, lights, greenDuration, yellowDuration);
        }

    }

}

class TrafficLight {
    private final Direction direction;
    private LightColor currentColor;
    private SignalState currentState;
    private SignalState nextState;
    private final List<TrafficObserver> observers = new ArrayList<>();
    private final int intersectionId;

    public TrafficLight(int intersectionId, Direction direction) {
        this.intersectionId = intersectionId;
        this.direction = direction;
        this.currentColor = LightColor.RED;
        this.currentState = new RedState();
        this.nextState = new GreenState();
    }

    public void startGreen() {
        this.currentState = new GreenState();
        this.currentState.handle(this);
    }

    public void transition() {
        this.currentState = this.nextState;
        this.currentState.handle(this);
    }

    public void setColor(LightColor color) {
        if (this.currentColor != color) {
            this.currentColor = color;
            notifyObservers();
        }
    }

    public void setNextState(SignalState state) {
        this.nextState = state;
    }

    public LightColor getCurrentColor() {
        return currentColor;
    }

    public Direction getDirection() {
        return direction;
    }

    public void addObserver(TrafficObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(TrafficObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers() {
        for (TrafficObserver observer : observers) {
            observer.update(intersectionId, direction, currentColor);
        }
    }

}

enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST
}

enum LightColor {
    GREEN,
    YELLOW,
    RED
}

interface IntersectionState {
    void handle(IntersectionController context) throws InterruptedException;
}

class EastWestGreenState implements IntersectionState {
    @Override
    public void handle(IntersectionController context) throws InterruptedException {
        System.out.printf("\n --- INTERSECTION %d: EAST-WEST GREEN ---\n", context.getId());
        context.getLight(Direction.EAST).startGreen();
        context.getLight(Direction.WEST).startGreen();

        context.getLight(Direction.NORTH).setColor(LightColor.RED);
        context.getLight(Direction.SOUTH).setColor(LightColor.RED);

        Thread.sleep(context.getGreenDuration());
        if (Thread.currentThread().isInterrupted()) return;

        context.getLight(Direction.EAST).transition();
        context.getLight(Direction.WEST).transition();

        Thread.sleep(context.getYellowDuration());
        if (Thread.currentThread().isInterrupted()) return;

        context.getLight(Direction.EAST).transition();
        context.getLight(Direction.WEST).transition();

        context.setState(new NorthSouthGreenState());
    }
}

class NorthSouthGreenState implements IntersectionState {
    @Override
    public void handle(IntersectionController context) throws InterruptedException {
        System.out.printf("\n--- INTERSECTION %d: Cycle Start -> North-South GREEN ---\n", context.getId());

        // Turn North and South green, ensure East and West are red
        context.getLight(Direction.NORTH).startGreen();
        context.getLight(Direction.SOUTH).startGreen();
        context.getLight(Direction.EAST).setColor(LightColor.RED);
        context.getLight(Direction.WEST).setColor(LightColor.RED);

        // Wait for green light duration
        Thread.sleep(context.getGreenDuration());
        if (Thread.currentThread().isInterrupted()) return;

        // Transition North and South to Yellow
        context.getLight(Direction.NORTH).transition();
        context.getLight(Direction.SOUTH).transition();

        // Wait for yellow light duration
        Thread.sleep(context.getYellowDuration());
        if (Thread.currentThread().isInterrupted()) return;

        // Transition North and South to Red
        context.getLight(Direction.NORTH).transition();
        context.getLight(Direction.SOUTH).transition();

        // Change the intersection's state to let East-West go
        context.setState(new EastWestGreenState());
    }
}

interface SignalState {
    void handle(TrafficLight context);
}

class GreenState implements SignalState {
    @Override
    public void handle(TrafficLight context) {
        context.setColor(LightColor.GREEN);
        context.setNextState(new YellowState());
    }
}

class RedState implements SignalState {
    @Override
    public void handle(TrafficLight context) {
        context.setColor(LightColor.RED);
        // Red is a stable state, it transitions to green only when the intersection
        // controller commands it.
        // So, the next state is self.
        context.setNextState(new RedState());
    }
}

class YellowState implements SignalState {
    @Override
    public void handle(TrafficLight context) {
        context.setColor(LightColor.YELLOW);
        // After being yellow, the next state is red.
        context.setNextState(new RedState());
    }
}

interface TrafficObserver {
    void update(int intersectionId, Direction direction, LightColor color);
}

class CentralMonitor implements TrafficObserver {
    @Override
    public void update(int intersectionId, Direction direction, LightColor color) {
        System.out.printf("[MONITOR] Intersection %d: Light for %s direction changed to %s.\n",
                intersectionId, direction, color);
    }
}
