package com.me.demo.bs;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

@WebService(serviceName = "MessageEngineService", endpointInterface = "com.me.demo", targetNamespace = "http://service.me.com")
public interface SOAPService {
    //从入参取topic
    @WebMethod
    String syncWithTopic(@WebParam(name = "topic") String topic, @WebParam(name = "msg") String msg);
    @WebMethod
    String asyncWithTopic(@WebParam(name = "topic") String topic, @WebParam(name = "msg") String msg);

    //从url取topic
    @WebMethod
    String syncWithUrl(@WebParam(name = "msg") String msg);
    @WebMethod
    String asyncWithUrl(@WebParam(name = "msg") String msg);
}
