import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TickTacToeDemo {
    
    public static void main(String[] args){
        TicTacToeSystem system = TicTacToeSystem.getInstance();

        Player alice = new Player("Alice",Symbol.X);
        Player bob = new Player("BOB",Symbol.O);

        System.out.println("---Game 1: Alice(X) vs Bob(O)------");
        system.createGame(alice,bob);
        system.printBoard();

        system.makeMove(alice,0,0);
        system.makeMove(bob,1,0);
        system.makeMove(alice,0,1);
        system.makeMove(bob,1,1);
        system.makeMove(alice,0,2);
        System.out.println("------------------\n");

        System.out.println("---Game 2 : Alice (O) vs Bob(X)---");
        system.createGame(alice,bob);
        system.printBoard();

        system.makeMove(alice,0,0);
        system.makeMove(bob,1,0);
        system.makeMove(alice,0,1);
        system.makeMove(bob,1,1);
        system.makeMove(alice,2,2);
        system.makeMove(bob,1,2);
        System.out.println("-----------------------\n");

        system.printScoreBoard();

    }
}

class TicTacToeSystem{
    private static volatile TicTacToeSystem instance;
    private Game game;
    private final ScoreBoard scoreBoard;

    public TicTacToeSystem(){
        this.scoreBoard = new ScoreBoard();
    }
    public static synchronized TicTacToeSystem getInstance(){
        if(instance == null){
            instance = new TicTacToeSystem();

        }
        return instance;
    }

    public void createGame(Player player1,Player player2){
        this.game = new Game(player1,player2);
        this.game.addObserver(this.scoreBoard);
        System.out.printf("Game started between %s (X) and %s (O) . %n",player1.getName(),player2.getName());
    }

    public void makeMove(Player player, int row, int col){
        if(game == null){
            System.out.println("No game in progress, create game frst\n");
            return;
        }
        try{
            System.out.printf("%s plays at (%d,%d)%n",player.getName(),row,col);
            game.makeMove(player,row,col);
            printBoard();
            System.out.println("Game status: " + game.getStatus());
            if(game.getWinner()!=null){
                System.out.println("Winner: " + game.getWinner().getName());
            }
        }
        catch(InvalidMoveException e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void printBoard(){
        game.getBoard().printBoard();
    }
    public void printScoreBoard(){
        scoreBoard.printScores();
    }


}


class Game extends GameSubject{
    private final Board board;
    private final Player player1;
    private final Player player2;
    private Player currentPlayer;
    private Player winner;
    private GameStatus gameStatus;
    private GameState state;
    private final List<WinningStrategy> winningStrategies;

    public Game(Player player1,Player player2){
        this.board=new Board(3);
        this.player1=player1;
        this.player2=player2;
        this.currentPlayer=player1;
        this.gameStatus=GameStatus.IN_PROGESS;
        this.state=new InProgressState();
        this.winningStrategies = List.of(
            new RowWinningStrategy(),
            new ColumnWinningStrategy(),
            new DiagonalWinningStrategy()
        );
    }
    public void makeMove(Player player,int row,int col){
        state.handleMove(this,player,row,col);
    }

    public boolean checkWinner(Player player){
        for(WinningStrategy strategy : winningStrategies){
            if(strategy.checkWinner(board,player)){
                return true;
            }
        }
        return false;
    }

    public void switchPlayer(){
        this.currentPlayer= (currentPlayer==player1)?player2:player1;
    }

    public Board getBoard(){ return board;}
        public Player getCurrentPlayer() {return currentPlayer;}
        public Player getWinner(){ return winner;}
        public void setWinner(Player winner){this.winner = winner;}
        public GameStatus getStatus() {return gameStatus;}
        public void setState(GameState state){
            this.state = state;
        }
        public void setStatus(GameStatus gameStatus){
            this.gameStatus = gameStatus;
            if(gameStatus!=GameStatus.IN_PROGESS){
                notifyObserver();
            }
        }

    
}

enum GameStatus{
    IN_PROGESS,
    WINNER_X,
    WINNER_O,
    DRAW
}

enum Symbol{
    X('X'),
    O('O'),
    EMPTY('_');

    private final char symbol;
    Symbol(char symbol){
        this.symbol = symbol;
    }
    public char getChar(){
        return symbol;
    }
}
//=============Exception handling====

class InvalidMoveException extends RuntimeException{
    public InvalidMoveException (String message){
        super(message);
    }
}

class Board{
    private final int size;
    private  int movesCount;
    private final Cell[][] board;

    public Board(int size){
        this.size=size;
        this.board=new Cell[size][size];
        movesCount = 0;
        initializeBoard();
    }
    public void initializeBoard(){
        for(int row = 0;row<size;row++){
            for(int col=0;col<size;col++){
                board[row][col]=new Cell();
            }
        }
    }

    public boolean placeSymbol(int row,int col,Symbol symbol){
        if(row<0 || row>size||col<0||col>size){
            throw new InvalidMoveException(" Invalid position : out of board");
        }
        if(board[row][col].getSymbol()!=Symbol.EMPTY){
            throw new InvalidMoveException("Invalid position : cell is already occupied");
        }
        board[row][col].setSymbol(symbol);
        movesCount++;
        return true;
    }

    public Cell getCell(int row,int col){
        if(row<0||row>size||col<0||col>size){
            return null;
        }
        return board[row][col];
    }
    public boolean isFull(){
        return movesCount == size*size;
    }

    public void printBoard(){
        System.out.println("----------------");
        for(int i=0;i<size;i++){
            System.out.print("| ");
            for(int j=0;j<size;j++){
                Symbol symbol =board[i][j].getSymbol();
                System.out.print(symbol.getChar() + "| ");
            }
            System.out.println("\n------------");
        }
    }
    

    public int getSize(){
        return size;
    }
}

class Cell{
    private Symbol symbol;
    public Cell(){
        this.symbol=Symbol.EMPTY;
    }
    public Symbol getSymbol(){
        return symbol;
    }
    public void setSymbol(Symbol symbol){
        this.symbol=symbol;
    }
}

class Player{
    private final String name;
    private final Symbol symbol;
    public Player(String name,Symbol symbol){
        this.name=name;
        this.symbol=symbol;
    }

    public String getName(){
        return name;
    }
   public Symbol getSymbol(){
    return symbol;
   }
}

interface GameObserver{
    void update(Game game);
}

class GameSubject{
    private final List<GameObserver> observers = new ArrayList<>();
    public void addObserver(GameObserver observer){
        observers.add(observer);
    }
    public void removeObserver(GameObserver observer){
        observers.remove(observer);
    }
    public void notifyObserver(){
        for(GameObserver observer : observers){
            observer.update((Game) this);
        }
    }

}

class ScoreBoard implements GameObserver{
    private final Map<String,Integer> scores;

    public ScoreBoard(){
        this.scores = new ConcurrentHashMap<>();
    }
    public void update(Game game){
        if(game.getWinner()!=null){
            String winnerName = game.getWinner().getName();
            scores.put(winnerName,scores.getOrDefault(winnerName, 0)+1);
            System.out.printf("[ScoreBoard] %s wins! Thier new score is %d %n", winnerName,scores.get(winnerName));
        }
    }

    public void printScores(){
        System.out.println("\n----Overall scoreboard----");
        if(scores.isEmpty()){
            System.out.print("No games with a winner have been player yet.");
            return;
        }
        scores.forEach((playerName,score)->
    System.out.printf("Player %-10ss | Wins: %d%n", playerName,score));
    System.out.println("---------\n");
    }
}

interface GameState {
    void handleMove(Game game, Player player, int row, int col);
}

class DrawState implements GameState {
    @Override
    public void handleMove(Game game, Player player, int row, int col) {
        throw new InvalidMoveException("Game is already over. It was a draw.");
    }
}

class InProgressState implements GameState {
    @Override
    public void handleMove(Game game, Player player, int row, int col) {
        if (game.getCurrentPlayer() != player) {
            throw new InvalidMoveException("Not your turn!");
        }

        // Place the piece on the board
        game.getBoard().placeSymbol(row, col, player.getSymbol());

        // Check for a winner or a draw
        if (game.checkWinner(player)) {
            game.setWinner(player);
            game.setStatus(player.getSymbol() == Symbol.X ? GameStatus.WINNER_X : GameStatus.WINNER_O);
            game.setState(new WinnerState());
        } else if (game.getBoard().isFull()) {
            game.setStatus(GameStatus.DRAW);
            game.setState(new DrawState());
        } else {
            // If the game is still in progress, switch players
            game.switchPlayer();
        }
    }
}

 class WinnerState implements GameState {
    @Override
    public void handleMove(Game game, Player player, int row, int col) {
        throw new InvalidMoveException("Game is already over. " + game.getWinner().getName() + " has won.");
    }
}

interface WinningStrategy {
    boolean checkWinner(Board board, Player player);
}
class ColumnWinningStrategy implements WinningStrategy {
    @Override
    public boolean checkWinner(Board board, Player player) {
        for (int col = 0; col < board.getSize(); col++) {
            boolean colWin = true;
            for (int row = 0; row < board.getSize(); row++) {
                if (board.getCell(row, col).getSymbol() != player.getSymbol()) {
                    colWin = false;
                    break;
                }
            }
            if (colWin) return true;
        }
        return false;
    }
}

class DiagonalWinningStrategy implements WinningStrategy {
    @Override
    public boolean checkWinner(Board board, Player player) {
        // Main diagonal
        boolean mainDiagWin = true;
        for (int i = 0; i < board.getSize(); i++) {
            if (board.getCell(i, i).getSymbol() != player.getSymbol()) {
                mainDiagWin = false;
                break;
            }
        }
        if (mainDiagWin) return true;

        // Anti-diagonal
        boolean antiDiagWin = true;
        for (int i = 0; i < board.getSize(); i++) {
            if (board.getCell(i, board.getSize() - 1 - i).getSymbol() != player.getSymbol()) {
                antiDiagWin = false;
                break;
            }
        }
        return antiDiagWin;
    }
}
 class RowWinningStrategy implements WinningStrategy {
    @Override
    public boolean checkWinner(Board board, Player player) {
        for (int row = 0; row < board.getSize(); row++) {
            boolean rowWin = true;
            for (int col = 0; col < board.getSize(); col++) {
                if (board.getCell(row, col).getSymbol() != player.getSymbol()) {
                    rowWin = false;
                    break;
                }
            }
            if (rowWin) return true;
        }
        return false;
    }
}