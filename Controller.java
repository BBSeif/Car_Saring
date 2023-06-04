

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class Controller {
    private final Connection connection;

    public  Controller(Connection connection) {
        this.connection = connection;
    }

    public Command mainMenu() {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Log in as a manager", this::managerMenu));
        menu.put(0, new MenuCommand("Exit", null));
        return displayMenu(menu);
    }

    public Command managerMenu() {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Company list", this::getCompanyList));
        menu.put(2, new MenuCommand("Create a company", this::createCompany));
        menu.put(0, new MenuCommand("Back", this::mainMenu));
        return displayMenu(menu);
    }

    public Command companyMenu(Company company) {
        System.out.println("'" + company.Name() + "' company");
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Car list", () -> getCarList(company)));
        menu.put(2, new MenuCommand("Create a car", () -> createCar(company)));
        menu.put(0, new MenuCommand("Back", this::managerMenu));
        return displayMenu(menu);
    }

    public Command displayMenu(Map<Integer,MenuCommand> menu) {
        menu.entrySet().stream()
                .map(i -> i.getKey() + ". " + i.getValue().getCaption())
                .forEach(System.out::println)
        ;
        System.out.print("> ");
        Scanner in = new Scanner(System.in);
        int key = in.nextInt();
        if (menu.containsKey(key)) {
            return menu.get(key).getCommand();
        }
        return null;
    }

    public Command getCompanyList() {
        System.out.println("Company list:");
        final String query = "select * from company order by id;";
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            if (!rs.next()) {
                System.out.println("The company list is empty");
                return () -> this::managerMenu;
            } else {
                do {
                    Company company = new Company(rs.getInt("id"), rs.getString("name"));
                    menu.put(company.Id(), new MenuCommand(company.Name(), () -> this.companyMenu(company)));
                } while (rs.next());
                menu.put(0, new MenuCommand("Back", this::managerMenu));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return displayMenu(menu);
    }

    public Command createCompany() {
        System.out.println("Enter the company name:");
        System.out.print("> ");
        Scanner in = new Scanner(System.in);
        String name = in.nextLine();
        final String query = "insert into company (name) values (?);";
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setString(1, name);
            st.executeUpdate();
            System.out.println("The company was created!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return () -> this::managerMenu;
    }

    public Command getCarList(Company company) {
        System.out.println("Car list:");
        final String query = "select * from car where company_id = ? order by id;";
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setInt(1, company.Id());
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                System.out.println("The car list is empty!");
            } else {
                int i = 1;
                do {
                    System.out.println(i + ". " + rs.getString("name"));
                    i++;
                } while (rs.next());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return () -> companyMenu(company);
    }

    public Command createCar(Company company) {
        System.out.println("Enter the car name:");
        System.out.print("> ");
        Scanner in = new Scanner(System.in);
        String name = in.nextLine();
        final String query = "insert into car (name, company_id) values (?, ?);";
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setString(1, name);
            st.setInt(2, company.Id());
            st.executeUpdate();
            System.out.println("The car was added!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return () -> companyMenu(company);
    }
}