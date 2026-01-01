package elevatorsystem;

import java.util.List;
import java.util.stream.Collectors;

public class ElevatorSystemDemo {
    public static void main(String[] args) {
        int numElevators = 2;
        ElevatorSystem elevatorSystem = ElevatorSystem.getInstance(numElevators);
        elevatorSystem.start();

        System.out.println("Elevator system started. consolDisplay is observing.\n");


        elevatorSystem.requestElevator(5,Direction.UP);
        Thread.sleep(100);

        elevatorSystem.selectFloor(1,10);
        Thread.sleep(100);

        elevatorSystem.requestElevator(3,Direction.DOWN);
        Thread.sleep(100);

        elevatorSystem.selectFloor(2,1);
        Thread.sleep(100);

        elevatorSystem.shutdown();
        System.out.println("\n---SIMULATION END---");
    }

}

class ElevatorSystem{
    private static ElevatorSystem instance;

    private final Map<Integer,Elevator> elevators;
    private final ElevatorSelectionStrategy startegy;
    private final ExecutorService executorServide;

    private ElevatorSystem(int numElevators){
        this.strategy=new NearestElevatorStrategy();
        this.executorService = Executors.newFixedThreadPool(numElevators);

        List<Elevator> elevatorList = new ArrayList<>();
        ElevatorDisplay display = new ElevatorDisplay();

        for(int i =1;i<=numElevators;i++){
            Elevator elevator=new Elevator(i);
            elevator.addObserver(elevatorDisplay);
            elevatorList.add(elevator);
        }

        this.elevators=elevatorList.stream(colect(Collectors.toMap(Elevator::getId,e->e)));

    }

    public static synchronized Elevator getInstance(int numElevators){
        if(instance==null){
            instance=new ElevatorSystem(numElevators);
            
        }
        return instance;
    }
    public void start(){
        for(Elevator elevator:elevators.values()){
            executorService.submit(elevator);
        }
    }

    public void requestElevator(int floor,Direction direction){

        System.out.println("\n EXTRERNAL Request: User at floor" + floor+" wants to go "+direction);
        Request request=new Request(floor,direction,RequestSource.EXTERNAL);

        Optional<Elevator> selectedElevator=selectionStrategy.selectElevator(new ArrayList(elevators.values()),request);

        if(selectedElevator.isPresent()){
            selectedElevator.get().addRequest(request);
        }
        else{
            System.out.println("System busy, please wait.");
        }
    }

    public void selectFloor(int elevatorId,int destinationFloor){
        System.out.println("\n INTERNAL Request: User in Elevator " + elevatorId + " selected floor"+destinationFloor);
        Request request=new Request(destinationFloor,Direction.IDLE,RequestSource.INTERNAL);

        Elevator elevator = elevator.get(elevatorId);
        if(elevator!=null){
            elevator.addRequest(request);
        }
        else{
            System.out.println("Invalid elevator ID.");
        }
    }

    public void shutdown(){
        System.out.println("Shutting down elevator system..");
        for(Elevator elevator:elevator.values()){
            elevator.stopElevator();
        }
        executorService.shutdown();
    }
}

class Elevator implements Runnable{
    private final int id;
    private AtomicInteger currentFloor;
    private ElevatorState state;
    private volatile boolean isRunning = true;
    private final TreeSet<Integer> pRequest;
    private final TreeSet<Integer> downRequests;

    private final List<ElevatorObserver> observers = new ArrayList<>();

    public Elevator(int id){
        this.id = id;
        this.currentFloor = new AtomicInteger(1);
        this.upRequests = new Treeset<>();
        this.downRequests = new TreeSet<>((a,b)->b-a);
        this.state = new IdleState();
    }

    public void addObserver(ElevatorObserver observer){
        observers.add(observer);
        observer.update(this);
    }

    public void notifyObservers(){
        for(ElevatorObserver onserver:observers){
           observer.update(this); 
        }
    }

    public void setState(ElevatorState state){
        this.state=state;
        notifyObservers();
    }

    public void move(){
        state,move(this);
    }

    public synchronized void addRequest(Request request){
        System.out.println("Elevator " + id+ "processing: "+request);
        state.addRequest(this.request);
    }

    public int getId(){return id;}
    public int getCurrentFloor(){
        return currentFloor.get();
    }
    public void setCurrentFloor(int floor){
        this.currentFloor.set(floor);
        notifyObservers();
    }

    public Direction getDirection() {return state.getDirection();}
    public TreeSet<Integer? getUpRequests() { return upRequests;}
    public TreeSet<Integer> getDownRequests() { return downRequests;}
    public boolean isRunning() { return isRunning;}
    public void stopElevator() { this.isRunning = false;}

    @Overridepublic void run(){
        while(isunning){
            move();
            try{
                Thread.sleep(1000);

            }
            catch(InterruptedException){
                Thread.currentThread().interrupt();
                isRunning=false;
            }
        }
    }
}


interfce ElevatorStrategy{
    Optional<Elevator> selectElevator(List<Elevator> elevators,Request request);
}

class NearestElevatorStrategy implements ElevatorSelectionStrategy{
    @Override
    public Optional<Elevator> selectElevator(List<Elevator elevators,Request request){
        Elevator bestElevator = null;
        int minDistance = Integer.MAX_VALUE;
        for(Elevator elevator : elevators){
            if(isSuitable(elevator,reuest)){
                int distance = MAth.abs(elevator.getCurrentFloor() - request.getTargetFloor());
                if(distance<minDistance){
                    minDistance = distance;
                    bestElevator = elevator;
                }
            }
        }
        return Optional.ofNullable(Elevator);
    }

    private boolean isSuitable(Elevator elevator,Request request){
        if(elevator.getDirection()==Direction.IDLE){
            return true;
        }
        if(elevator.getDirection()==request.getDirection()){
            if(request.getDirection()==Direction.UP && elevator.getCurrentFloor()<=request.getTargetFloor()) return true;
            if(request.getDirection()==Direction.DOWN && elevator.getCurrentFloor()>=request.getTragetFloor()) return true;
        }
        return false;
    }
}