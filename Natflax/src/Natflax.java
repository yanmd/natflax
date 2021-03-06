import java.sql.*;
import java.util.*;
import org.h2.*;


public class Natflax {

    static Store store;
    public static void main(String[] args)
        throws Exception{
        //Connection conn = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
        //Statement statement = conn.createStatement();
        //statement.executeQuery("SELECT * FROM Employees");
        Database.connectToDatabase(
                "jdbc:h2:~/test",
                "natflax",
                "admin");

        startDBQuery();
        Database.close();
    }
    
    private static void startDBQuery()
        throws Exception{
        System.out.println("Welcome to NATFLAX!\n" +
                "Are you a(n):\n" +
                "\t1-Customer\n" +
                "\t2-Employee\n" +
                "\t3-New\n" +
                "\t4-Quit");
        Scanner in = new Scanner(System.in);
        try {
            int login = in.nextInt();
            if(login == 1 | login == 2)
                login(login);
            switch(login){
                case(3):
                    //Create new login
                    CreateNewCustomer();
                    break;
                case(4):
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Command not recognized");
                    startDBQuery();

            }
        }catch (InputMismatchException e){
            System.out.println("Input not in correct format, try again.");
            startDBQuery();
        }
    }

    private static void CreateNewCustomer()
        throws Exception {
        System.out.println("Welcome! Please fill out some basic information.");
        Customer c = new Customer();
        customerActions(c);//for now
        //customerLogin() //when it's implemented
    }


    private static void login(int action)
        throws Exception{
        System.out.println("Please log in.");
        System.out.println("Enter username:");
        Scanner in = new Scanner(System.in);
        try {
            String user = in.next();
            //actually log in somehow or whatever
            //if(logged in) c = customer that exists somehow
            switch(action){
                case(1):
                    ResultSet user_query = Database.queryDB("SELECT * FROM Customer WHERE username = '" + user + "'");
                    if(user_query.first() == false)
                    {
                        System.out.println("Login failed - user " + user + " not found");
                        login(action);
                    }
                    else
                    {
                        Customer sampleC;
                        int columns = user_query.getMetaData().getColumnCount();
                        String[] info = new String[columns];
                        for(int i = 0; i < columns; i++)
                        {
                            info[i] = user_query.getString(i+1);
                        }
                        
                        ResultSet cc_query = Database.queryDB("SELECT P.ccNumber, P.ccExpiration, P.ccPIN, P.ccSecurity FROM Customer natural join Payment as P WHERE Customer.username = '" + user + "'");
                        if(cc_query.first() == true)
                        {
                            columns = cc_query.getMetaData().getColumnCount();
                            String[] cc_info = new String[columns];
                            for(int i = 0; i < columns; i++)
                            {
                                cc_info[i] = cc_query.getString(i+1);
                            }
                            sampleC = new Customer(info, cc_info);
                        }
                        else
                        {
                            sampleC = new Customer(info);
                        }
                        customerActions(sampleC);
                    }
                    break;
                case(2):
                    ResultSet employee_query = Database.queryDB("SELECT * FROM Employee WHERE ID = '" + user + "'");
                    if(employee_query.first() == false)
                    {
                        System.out.println("Login failed - employee ID " + user + " not found");
                        login(action);
                    }
                    else
                    {
                        Employee sampleE;
                        int columns = employee_query.getMetaData().getColumnCount();
                        String[] info = new String[columns];
                        for(int i = 0; i < columns; i++)
                        {
                            info[i] = employee_query.getString(i+1);
                        }
                        // Find the store ID
                        ResultSet store_query = Database.queryDB("SELECT SID FROM Works_for WHERE ID = '" + user + "'");
                        if(store_query.first() == true)
                        {
                            String SID = store_query.getString(1);
                            sampleE = new Employee(info, SID);
                            
                            ResultSet manager_query = Database.queryDB("Select Manager_ID from Store where Manager_ID = '" + user + "';");
                            if(manager_query.isBeforeFirst())
                            {
                                managerActions(sampleE);
                            }
                            else
                            {
                                employeeActions(sampleE);
                            }
                        }
                        else
                        {
                            System.out.println("Critical error: Employee does not work for a store!");
                        }
                    }
                    break;
            }
        }catch (InputMismatchException e){
            System.out.println("Input not in correct format, try again.");
            login(action);
        }
    }

    private static void managerActions(Employee m)
        throws Exception{
        System.out.println("What would you like to do?\n" +
                "\t1-Return book\n" +
                "\t2-Return movie\n" +
                "\t3-Add book\n" +
                "\t4-Add movie\n" +
                "\t5-Edit Information\n" +
                "\t6-Add Employee\n" +
                "\t7-Fire Employee\n" +
                "\t8-Quit");
        Scanner in = new Scanner(System.in);
        int action = in.nextInt();
        switch(action){
            case(1):
                m.returnItem("book");
                managerActions(m);
                break;
            case(2):
                m.returnItem("movie");
                managerActions(m);
                break;
            case(3):
                m.addItem("book");
                managerActions(m);
                break;
            case(4):
                m.addItem("movie");
                managerActions(m);
                break;
            case(5):
                m.updateEmpInfo();
                managerActions(m);
                break;
            case(6):
                System.out.println("Fill out your information:");
                Employee e = new Employee(m.store);
                managerActions(m);
                break;
            case(7):
                System.out.println("List of employees that work for your store:");
                ResultSet employee_query = Database.queryDB("Select E.* from (Employee as E natural join works_for) " +
                                                            "where SID = '" + m.store + "';");
                Database.printResultSet(employee_query);
                System.out.println("Enter ID of Employee to fire (-1 to cancel):");
                
                String emp_id;
                ResultSet query_id;
                do
                {
                    emp_id = in.next();
                    if(emp_id.equals("-1"))
                    {
                        managerActions(m);
                        return;
                    }
                    query_id = Database.queryDB("Select * from Employee where ID = '" + emp_id + "';");
                    if(query_id.isBeforeFirst() == false)
                    {
                        System.out.println("That is not a valid employee ID.");
                    }
                }while(query_id.isBeforeFirst() == false);
                Database.updateDB("Delete from Works_for where ID = '" + emp_id +"';");
                Database.updateDB("Delete from Employee where ID = '" + emp_id +"';");
                managerActions(m);
                break;
            case(8):
                System.out.println("Goodbye!");
                break;
            default:
                System.out.println("Command not recognized");
                managerActions(m);
                break;
        }
    }

    private static void customerActions(Customer c) 
        throws Exception {
        System.out.println("What can we help you with?:\n" +
                "\t1-Search Books\n" +
                "\t2-Search Movies\n" +
                "\t3-Edit Information\n" +
                "\t4-Check Rentals\n" +
                "\t5-Quit");
        Scanner in = new Scanner(System.in);
        try{
            int action = in.nextInt();
            switch (action) {
                case (1):
                    beginRental(c, "book");
                    break;
                case (2):
                    beginRental(c, "movie");
                    break;
                case (3):
                    c.updateCustInfo();
                    customerActions(c);
                    break;
                case (4):
                    c.checkRentals();
                    customerActions(c);
                    break;
                case (5):
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Command not recognized");
                    customerActions(c);
                    break;
            }
        }catch (InputMismatchException e){
            System.out.println("Input not in right format, try again.");
            customerActions(c);
        }
    }
    private static void beginRental(Customer c, String type)
            throws Exception
    {
        String table, stock_table;
        if(type.equalsIgnoreCase("movie"))
        {
            table = "Movie";
            stock_table = "Movies_in_stock";
        }
        else
        {
            table = "Book";
            stock_table = "Books_in_stock";
        }

        Scanner in = new Scanner(System.in);
        System.out.println("Input search keyword:");
        
        String search = in.next();
        ResultSet item_query = Database.queryDB("SELECT distinct " + table + ".* from " + table + " natural join " + stock_table + " where stock > 0 and title like '%" + search + "%'");

        if(Database.printResultSet(item_query) == false)
        {
            // Could not find any books in stock
            item_query = Database.queryDB("SELECT * from " + table + " where title like '%" + search + "%'");
            if(item_query.isBeforeFirst() == true)
            {
                Database.printResultSet(item_query);
                System.out.println("The above " + type + "s were found, but not in stock.\n");
            }
            else
            {
                System.out.println("No " + type + "s were found with a title like that.\n");
            }
            customerActions(c);
        }
        else
        {
            System.out.println("\nWould you like to check out a title? (yes/no)");
            String response = in.next();
            if(response.equals("yes"))
            {
                c.checkOut(type);
                customerActions(c);
            }
            else if(response.equals("no"))
            {
                customerActions(c);
            }
            else 
            {
                System.out.println("not recognized, assuming no");
                customerActions(c);
            }
        }
    }


    private static void employeeActions(Employee e)
        throws Exception{
        System.out.println("What would you like to do?\n" +
                "\t1-Return book\n" +
                "\t2-Return movie\n" +
                "\t3-Add book\n" +
                "\t4-Add movie\n" +
                "\t5-Edit Information\n" +
                "\t6-Quit");
        Scanner in = new Scanner(System.in);
        int action = in.nextInt();
        switch(action){
            case(1):
                e.returnItem("book");
                employeeActions(e);
                break;
            case(2):
                e.returnItem("movie");
                employeeActions(e);
                break;
            case(3):
                e.addItem("book");
                employeeActions(e);
                break;
            case(4):
                e.addItem("movie");
                employeeActions(e);
                break;
            case(5):
                e.updateEmpInfo();
                employeeActions(e);
                break;
            case(6):
                System.out.println("Goodbye!");
                break;
            default:
                System.out.println("Command not recognized");
                employeeActions(e);
                break;
        }
    }
    private static void beginReturn(Employee e, String type)
    {

    }
}