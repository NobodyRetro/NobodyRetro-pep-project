package Controller;

import java.sql.PreparedStatement;
import java.sql.ResultSet; //So I can get returned keys from SQL
import java.sql.SQLException;

import Util.ConnectionUtil; //so I can use ConnectionUtil.java
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.sql.Statement; //So I can create generated keys in the database
import java.util.ArrayList; //So I can use ArrayLists
import java.util.List; //So I can use lists

import javax.xml.transform.Result;

import java.sql.Connection;
import Model.Account; //so I can use everything from Account.java
import Model.Message; //So I can use everything in Massage.java

/**
 * TODO: You will need to write your own endpoints and handlers for your controller. The endpoints you will need can be
 * found in readme.md as well as the test cases. You should
 * refer to prior mini-project labs and lecture materials for guidance on how a controller may be built.
 */
public class SocialMediaController {
    /**
     * In order for the test cases to work, you will need to write the endpoints in the startAPI() method, as the test
     * suite must receive a Javalin object from this method.
     * @return a Javalin app object which defines the behavior of the Javalin controller.
     */
    public Javalin startAPI() {
        Javalin app = Javalin.create();
        app.get("example-endpoint", this::exampleHandler);
        
    //REGISTER TESTS 
        app.post("register", this::handleRegister);

    //LOGIN TESTS
        app.post("login", this::loginHandler);

    //CREATE & RETRIEVE ALL MESSAGE TESTS
        app.post("messages", this::messagesHandler);

    //DELETE MESSAGE TESTS
        app.post("messages/:id", this::messagesDeleteHandler);

    //RETRIEVE USER MESSAGES TESTS
        app.post("accounts/1/messages", this::userMessagesHandler);

    //RETRIEVE MESSAGES FROM ID TESTS
        app.post("messages/100", this::messagesIDHandler);

    //UPDATE MESSAGES TESTS
        app.post("messages/2", this::messagesUpdateHandler);

		return app;
    }

    private void handleRegister(Context context) throws SQLException{
//Connect to Account.java, convert the body sent from the client into the account.class
        Account account = context.bodyAsClass(Account.class); 
//Check username !null & password length > 4
        if(account.getUsername().isEmpty() || account.getPassword().length() < 4){
            context.status(400); return;
        }

        Connection handleConnection = ConnectionUtil.getConnection(); 
//prepared statment creates the username, password and key
        PreparedStatement ps = handleConnection.prepareStatement
        ("insert into account (username,password) values (?,?)", 
        Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, account.getUsername());
        ps.setString(2, account.getPassword());
    

        try{
//try to create the account, gets generated keys, gets the number, returns the account then a 200 status
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            account.setAccount_id(rs.getInt(1));
            context.json(account);
            context.status(200); return;

//if it exists then catch and send 400 status
        } catch (SQLException e) { 
            context.status(400); return;
        }
          
    }
    
    private void loginHandler(Context context) {
//Connect to Account.java, convert the body sent from the client into the account.class
        Account account = context.bodyAsClass(Account.class);
        if(account == null) {
            context.status(400);
            return;
        }
//Create variables to use
        String username = account.getUsername();
        String password = account.getPassword();
        Connection connection = ConnectionUtil.getConnection();
    
        try {
//Check if the username from the client is in the database
            PreparedStatement ps = connection.prepareStatement("SELECT account_id FROM account WHERE username = ? and password = ?");
            ps.setString(1, username);
            ps.setString(2,password);
            ResultSet resultSet = ps.executeQuery();
    
//If the username exists check to see if the password is correct
            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");

//Successful login
                    int accountID = resultSet.getInt("account_id");
                    account.setAccount_id(accountID);
                    context.json(account);
                    context.status(200);
                    return;
            }
    
// If username doesn't exist or password is incorrect, return 401 Unauthorized
            context.status(401);
        } catch (SQLException e) {
            e.printStackTrace();
// Handle the exception if necessary
            context.status(500); 
        }
    }

    private void messagesHandler(Context context){
        try{
//Try to connect to everything like we have been doing
            Connection connection = ConnectionUtil.getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM message");
            ResultSet resultSet = ps.executeQuery();
//Create an ArrayList called messages
            List<Message> messages = new ArrayList<>();
            while (resultSet.next()){

//Set variables to the resultSet
                int messageID = resultSet.getInt("message_id");
                int accountID = resultSet.getInt("posted_by");
                String messageText = resultSet.getString("message_text");
                long timestamp = resultSet.getLong("time_posted_epoch");

//Check for empty message and mesage length
                if(messageText.isEmpty() || messageText.length() > 254){
                    context.status(400);
                    return;
                }

//Check if the user exists in the database
                PreparedStatement userCheck = connection.prepareStatement("SELECT * FROM account WHERE account_id=?");
                userCheck.setInt(1,accountID);
                ResultSet userCheckResult = userCheck.executeQuery();
                if(!userCheckResult.next()){
                    context.status(400);
                    return;
                }

                Message message = new Message(messageID, accountID, messageText, timestamp);
                messages.add(message);
                
            }
            context.json(messages);
            context.status(200);

        } catch (SQLException e){
            e.printStackTrace();
            context.status(500);
        }

    }

    private void messagesDeleteHandler (Context context) {
        try{
//Get the message ID from the URL parameted ( '1', '100')
            String messageIDString = context.pathParam("id");
            int messsageID = Integer.parseInt(messageIDString);
//Connect to DB
            Connection connection = ConnectionUtil.getConnection();
//Check is the message exists within the given ID
            PreparedStatement checkMessageExists = connection.prepareStatement("SELECT * FROM message WHERE message_id=?");
            checkMessageExists.setInt(1, messsageID);
            ResultSet resultSet = checkMessageExists.executeQuery();
//If it does, delete it
            if(resultSet.next()){
                PreparedStatement deleteMessage = connection.prepareStatement("DELETE * FROM messages WHERE message_id=?");
                deleteMessage.setInt(1,messsageID);
                int rowsAffected = deleteMessage.executeUpdate();

                if(rowsAffected == 1){
                    context.status(200);
                }else{
                    context.status(500);
                }
            }else{
//Message with given ID doesnt exist
                context.status(200);
            }
        } catch (SQLException | NumberFormatException e){
            e.printStackTrace();
            context.status(500);
        }

    }

    private void userMessagesHandler (Context context) {

    }

    private void messagesIDHandler (Context context) {

    }

    private void messagesUpdateHandler (Context context) {

    }

    private void prepareStatement(String string) {

    }





    /**
     * This is an example handler for an example endpoint.
     * @param context The Javalin Context object manages information about both the HTTP request and response.
     */
    private void exampleHandler(Context context) {
        context.json("sample text");
    }


}