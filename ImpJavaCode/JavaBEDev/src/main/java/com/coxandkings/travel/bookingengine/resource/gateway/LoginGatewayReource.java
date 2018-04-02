package com.coxandkings.travel.bookingengine.resource.gateway;

public class LoginGatewayReource {
    //variables
        private String username;

        private String password;
    //setter getter
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "ClassPojo [username = " + username + ", password = " + password + "]";
    }
}
