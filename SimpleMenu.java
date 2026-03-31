import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.input.*;

public class SimpleMenu {
    public static void main(String[] args) throws Exception {
        Terminal t = new DefaultTerminalFactory().createTerminal();

        Screen s = new TerminalScreen(t);


            KeyType kt = s.readInput().getKeyType();
       
            if(kt == KeyType.ArrowDown)
            {
                System.out.println("ARROW DOWN");
            }
            if(kt == KeyType.ArrowUp)
            {
                System.out.println("ARROW UP");
            }
            if(kt == KeyType.ArrowLeft)
            {
                System.out.println("left");
            }
            if(kt == KeyType.ArrowUp)
            {
                System.out.println("upp");
            }
            


        


        
    }
}