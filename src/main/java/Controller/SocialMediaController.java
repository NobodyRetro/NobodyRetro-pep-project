package Controller;

import java.sql.PreparedStatement;
import java.sql.ResultSet; //So i can get returned keys from SQL
import java.sql.SQLException;

import Util.ConnectionUtil; //so I can use ConnectionUtil.java
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.sql.Statement; //So we can create generated keys in the database

import javax.xml.transform.Result;

import java.sql.Connection;
import Model.Account; //so I can use the get/setUsername and get/setPassword

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
        app.post("messages/1", this::messagesDeleteHandler);

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

    }

    private void messagesHandler(Context context){

    }

    private void messagesDeleteHandler (Context context) {

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