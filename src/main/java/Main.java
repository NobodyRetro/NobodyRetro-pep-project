import Controller.SocialMediaController;
import io.javalin.Javalin;

/**
 * This class is provided with a main method to allow you to manually run and test your application. This class will not
 * affect your program in any way and you may write whatever code you like here.
 */


 // User Registration                                     
 // Login                                                            
 // Create New Message                                    
 // Get All Messages
 // Get One Message Given Message Id
 // Delete a Message Given Message Id
 // Update Message Given Message Id
 // Get All Messages From User Given Account Id 

public class Main {
    public static void main(String[] args) {
        SocialMediaController controller = new SocialMediaController();
        Javalin app = controller.startAPI();
        app.start(8080);
    }
}
