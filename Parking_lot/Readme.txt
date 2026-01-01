ParkingLot system:==

This is considered easy level question in  lld

Basically we have two types of parking lots,
1. in single florr with multiple gates
2. in multiple floors

the one i provided is multiple floors.

These two types will differ only in concurrency handling, but in the code the design patterns, and principles used are same.

Imp design patterns used in this are:=

1. Signleton pattern = to create ParkingLot singleton class. because we only have one parkinglot, if you have multiple it wont be a singleton class.
2. Strategy pattern = to be able to include different types of parking Strategy in our words, means how exactly you want to search your parking slot?, so there might be diff strategies so, we use strategy pattern it uses interface , and we can create multiple concrete classes, we used same pattern for payment.
3. Factory pattern = to create vehicle class, we can have diff types of vehicles, 

diff between factory and strategy design patterns

=> we can use volatile key for vehicle visibility, and reentantlocks for syncronization.

