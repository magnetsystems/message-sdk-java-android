package com.magnet.max.common;

public class ClientRegistrationRequest {

    private String tag;
    private String client_name;
   // private String client_url;
    private String client_description;
//    private String redirect_url;
    private String password;

    private String owner_email;

    public ClientRegistrationRequest(){

    }

    public String getClient_description() {
        return client_description;
    }

    public void setClient_description(String client_description) {
        this.client_description = client_description;
    }


    public String getClient_name() {
        return client_name;
    }

    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

//    public String getClient_url() {
//        return client_url;
//    }
//
//    public void setClient_url(String client_url) {
//        this.client_url = client_url;
//    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getOwner_email() {
        return owner_email;
    }

    public void setOwner_email(String owner_email) {
        this.owner_email = owner_email;
    }
}
