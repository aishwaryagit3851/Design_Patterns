package Task_Management;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskManagementDemoAish {
       
   
    public static void main(String[] args) {
        TaskManager taskManager = TaskManager.getInstance();
       


        User user1=taskManager.createUser(1,"kanama","kanama@gmail.com");
        
        User user2=taskManager.createUser(2,"bangaram","bangaram@gmail.com");
        System.out.println("User created: " + user1);

        Task task1 = taskManager.createTask(1, "Design Module", TaskPriority.HIGH, user1, LocalDate.now().plusDays(7));
        System.out.println("Task created: " + task1);

        user1.assignTo(user2, task1);
        System.out.println("Task assigned to: " + user2);   
        
        task1.addComment(user1, " Task is assigned to " + user2.name + " please start working on it.");
        System.out.println("Comment added to task: " + task1+" by "+user1.name+"comment");

        task1=taskmanager.update

        

    }
}

class TaskManager{
    private static TaskManager taskmanager = new TaskManager();
private List<Task> tasks ;
    private List<User> users;
    private TaskManager(){
        tasks=new ArrayList<>();
        users=new ArrayList<>();
    }
    public static synchronized TaskManager getInstance(){
        return taskmanager;
    }   
    public User createUser(int id, String name,String email){
        User user = new User(id,name,email);
        users.add(user);
        return user;
    }
    public Task createTask(int id, String title,TaskPriority priority, User assignedUser, LocalDate dueDate){
        Task task = new Task(id,title,priority,assignedUser,dueDate);
        //set task properties
        tasks.add(task);
        return task;

    }

}

class Task{
    private int id;
    private String title;
  //  private TaskState currentState;
    private User assignedUser;
    private User assignee;
   // private Comment comment;
    private LocalDate dueDate;
   // private List<AcitivityLog> activityLogs;
   // private Set<Tag> tags;
   // private List<TaskObserver> observers;
   private TaskPriority priority;
    private List<Comment> comments;

   Task(int id, String title,TaskPriority priority, User assignedUser, LocalDate dueDate){
    this.id=id;
    this.title=title;
    this.priority=priority;
    this.assignedUser=assignedUser;
    this.dueDate=dueDate;
    comments=new CopyOnWriteArrayList<>();
   }
    // public void setObserver(TaskObserver observer){
    //     observers.addObserver(observer);
    // }

   public void setassignee(User user) {
        this.assignee = user;
   }
    public void addComment(User user1, String string) {
        Comment comment=new Comment(1,string, new Date(System.currentTimeMillis()),user1);
        comments.add(comment);
    }

}

class User{
    int id;
    String name;
    String email;
    User(int id,String name,String email){
        this.id=id;
        this.name=name;
        this.email=email;
    }

   

    public void assignTo(User user,Task task){

        task.setassignee(user);
    }
}
enum TaskPriority{
LOW,
MEDIUM,
HIGH,
CITICAL
}

enum TaskStatus{
    TO_DO,
    IN_PROGRESS,
    REVIEW,
    DONE
}

class Comment{
    int id;
    String content;
    Date timestamp;
    User author;
    
    Comment(int id, String content, Date timestamp,User author){
        this.id=id;
        this.content=content;
        this.timestamp=timestamp;
        this.author=author;
     
    }
}