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
        app.post("/register", this::handleRegister);

//LOGIN TESTS
        app.post("/login", this::loginHandler);

//CREATE & RETRIEVE ALL MESSAGE TESTS
        app.get("/messages", this::messagesHandler);

//DELETE MESSAGE TESTS
        app.delete("/messages/delete/{id}", this::messagesDeleteHandler);

//RETRIEVE USER MESSAGES TESTS
        app.get("accounts/{id}/messages", this::userMessagesHandler);

//RETRIEVE MESSAGES FROM ID TESTS
        app.get("messages/id/{id}", this::messagesIDHandler);

//UPDATE MESSAGES TESTS
        app.patch("messages/update/{id}", this::messagesUpdateHandler);

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
//*****************************************************************************************************************************/    

    private void loginHandler(Context context) {
//Connect to Account.java, convert the body sent from the client into the account.class
        Account account = context.bodyAsClass(Account.class);

//If the account is null or if getAccount_id is less than or equal to 0, 400 error code
//        if(account == null || account.getAccount_id() <= 0) {
//            context.status(401);
//            return;
//        }

//Create variables to use 
        int accountID = account.getAccount_id();
        String username = account.getUsername();
        String password = account.getPassword();

//Connect to the DB
        Connection connection = ConnectionUtil.getConnection();
    
        try {
//Check if the account_id from the client(testing) is in the database
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM account WHERE account_id=?");
            ps.setInt(1, accountID);
            //ps.setString(2, password);
            ResultSet resultSet = ps.executeQuery();
    
//If the username exists check to see if the password is correct
            if (resultSet.next()) {
                int storedAccountID = resultSet.getInt("account_id");
                if(accountID == storedAccountID){
                    account.setAccount_id((accountID));
                    context.json(account);
                    context.status(200);
                    return;
                }
                
            }
    
// If username doesn't exist or password is incorrect, return 401 
            context.status(401);
        } catch (SQLException e) {
            e.printStackTrace();
// Handle the exception if necessary
            context.status(500); 
        }
    }

//*****************************************************************************************************************************/    

    private void messagesHandler(Context context){
        try{
            Connection connection = ConnectionUtil.getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM message");
            ResultSet resultSet = ps.executeQuery();

//Create a list to store messages
            List<Message> messages = new ArrayList<>();

//Set variables to the resultSet
            while (resultSet.next()){
                int messageID = resultSet.getInt("message_id");
                int accountID = resultSet.getInt("posted_by");
                String messageText = resultSet.getString("message_text");
                long timestamp = resultSet.getLong("time_posted_epoch");

//Check for empty message and mesage length
                if(messageText.isEmpty() || messageText.length() > 254){
                    context.status(400);
                    return;
                }

//If exists make a constructor and return the message
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

 //*****************************************************************************************************************************/    
   
    private void messagesDeleteHandler (Context context) {
        try{
//Get the message ID from the URL path( '1', '100')
            String messageIDString = context.pathParam("id");
            int messsageID = Integer.parseInt(messageIDString);

//Connect to DB
            Connection connection = ConnectionUtil.getConnection();

//Check is the message exists within the given ID
            PreparedStatement checkMessageExists = connection.prepareStatement("SELECT * FROM message WHERE message_id=?");
            checkMessageExists.setInt(1, messsageID);
            ResultSet resultSet = checkMessageExists.executeQuery();

//Iterate a while loop and get up variables
            if(resultSet.next()){
                int accountID = resultSet.getInt("posted_by");
                String messageText = resultSet.getString("message_text");
                long timestamp = resultSet.getLong("time_posted_epoch");

                PreparedStatement deleteMessage = connection.prepareStatement("DELETE * FROM messages WHERE message_id=?");
                deleteMessage.setInt(1,messsageID);
                int rowsAffected = deleteMessage.executeUpdate();

//If rows have been affected then construct a Message object and return it as a json objext
                if(rowsAffected == 1){
                    Message deletedMessage = new Message(messsageID, accountID, messageText, timestamp);
                    context.json(deletedMessage);
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

//*****************************************************************************************************************************/    

    private void userMessagesHandler (Context context) {
//Connect to the path
        int accountId = Integer.parseInt(context.pathParam("id"));

        try {
//Connect and check if user exists within the DB using a prepared statement
            Connection connection = ConnectionUtil.getConnection();

/*(testing doesnt require a check to see if the user exists)
            PreparedStatement userCheckPS = connection.prepareStatement("SELECT * FROM users WHERE user_id = ?");
            userCheckPS.setInt(1, accountId);
            ResultSet userCheckResultSet = userCheckPS.executeQuery();
    
//User doesnt exist 
           if (!userCheckResultSet.next()) {
                context.status(404);
                return;
            }*/
    
//User exists, continue
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM message WHERE posted_by = ?");
            ps.setInt(1, accountId);
            ResultSet resultSet = ps.executeQuery();
    
            List<Message> messages = new ArrayList<>();
            while (resultSet.next()) {

//Varibales to use in Message.java
                int messageID = resultSet.getInt("message_id");
                int accountID = resultSet.getInt("posted_by");
                String messageText = resultSet.getString("message_text");
                long timestamp = resultSet.getLong("time_posted_epoch");
//Constructor for the message
                Message message = new Message(messageID, accountID, messageText, timestamp);
                messages.add(message);
            }
    
            context.json(messages);
            context.status(200);
        } catch (SQLException e) {
            e.printStackTrace();
            context.status(500);
        }
    }

//*****************************************************************************************************************************/    

    private void messagesIDHandler (Context context) {
        try {
//Get the messageID from the URL path
            String messageIDString = context.pathParam("id");
            int messageID = Integer.parseInt(messageIDString);

//Connect and create a PS targeting all messages from the given message id 
            Connection connection = ConnectionUtil.getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM message WHERE message_id = ?");
            ps.setInt(1, messageID);
            ResultSet resultSet = ps.executeQuery();
    
            if (resultSet.next()) {
//Create variables targeting Message.java
                int messageIDResult = resultSet.getInt("message_id");
                int accountID = resultSet.getInt("posted_by");
                String messageText = resultSet.getString("message_text");
                long timestamp = resultSet.getLong("time_posted_epoch");
//Constructor 
                Message message = new Message(messageIDResult, accountID, messageText, timestamp);
    
                context.json(message);
                context.status(200);
            } else {
//Message not found
                context.status(404); 
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            context.status(500);
        }
    }

//*****************************************************************************************************************************/    

    private void messagesUpdateHandler (Context context) {
        try {
//Get the message from the path, connect, create Prepared statement to update the message_text at the provided message_id location
            String messageIDString = context.pathParam("id");
            int messageID = Integer.parseInt(messageIDString);
            Connection connection = ConnectionUtil.getConnection();
            PreparedStatement ps = connection.prepareStatement("UPDATE message SET message_text = ? WHERE message_id = ?");
            
// Get the new message text from the request body
            String newMessageText = context.body();
    
//Check that the new message isnt empty or longer than 254, if they are send 400 
            if (newMessageText.isEmpty() || newMessageText.length() > 254) {
                context.status(400);
                return;
            }
//Parameters for the prepared statement 
            ps.setString(1, newMessageText);
            ps.setInt(2, messageID);

//Exectutes the update and returns the number of rows effected
            int rowsAffected = ps.executeUpdate();
 
//If a row was effected (meaning it was successful) do this
            if (rowsAffected == 1) {
                PreparedStatement retrieveUpdatedMessage = connection.prepareStatement("SELECT * FROM message WHERE message_id = ?");
                retrieveUpdatedMessage.setInt(1, messageID);
                ResultSet resultSet = retrieveUpdatedMessage.executeQuery();
    
//Assign variables to Message.java
                if (resultSet.next()) {
                    int updatedMessageID = resultSet.getInt("message_id");
                    int accountID = resultSet.getInt("posted_by");
                    String updatedMessageText = resultSet.getString("message_text");
                    long timestamp = resultSet.getLong("time_posted_epoch");
    
//Contructor makes a new message objected called updatedMessage, returns it as a json object and returns the status 200
                    Message updatedMessage = new Message(updatedMessageID, accountID, updatedMessageText, timestamp);
                    context.json(updatedMessage);
                    context.status(200);
                } else {

//Unable to retrieve updated message
                    context.status(500);
                }
            } else {

//No message was found, no rows were affected
                context.status(404);
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            context.status(500);
        }
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