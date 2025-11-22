package com.vogdo.mcpserver.tools;

import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class McpTools {

    // simple db pour le test
    private final List<Employee> employees = List.of(
            new Employee("Hassan", "fadli", "hfad@gmail.com", 12300, 4),
            new Employee("Mohamed", "fadli", "mfad@gmail.com", 34000, 1),
            new Employee("imane", "fadli", "ifad@gmail.com", 14000, 2)
    );

    @McpTool(
            name = "getEmployee",
            description = "Get information about a given employee by first name"
    )
    public Employee getEmployee(
            @McpArg(description = "The employee first name") String firstName) {
        if (firstName == null){
            return null;
        }

        String normalizedFirstName = firstName.trim().toLowerCase(Locale.ROOT);

        return employees.stream()
                .filter(e -> e.firstName().toLowerCase(Locale.ROOT).equals(normalizedFirstName))
                .findFirst()
                .orElse(null);
        //return new Employee(firstName, "","",0,0);
        //return new Employee(firstName, lastName, email, salary, seniority);
    }

    @McpTool(
            name = "getAllEmployee",
            description = "Get All information about all employees"
    )
    public List<Employee> getAllEmployees() {
        return employees;
    }

}

record Employee(String firstName, String lastName, String email, double salary, int seniority) {}
