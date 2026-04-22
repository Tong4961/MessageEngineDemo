package com.me.bs.soap;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName WebServiceConfig
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/20 10:37
 * @Version 1.0
 */
@Configuration
public class WebServiceConfig {
    @Autowired
    private Bus bus; //CXF核心总线 Starter会自动配置
    @Bean
    public ServletRegistrationBean disServlet() {
        ServletRegistrationBean bean = new ServletRegistrationBean(new CXFServlet(), "/soap/*");
        //ming 20230803 隐藏cxf available SOAP services服务列表 xxx/services
        bean.addInitParameter("hide-service-list-page", "true");
        return bean;
    }

    @Autowired
    private SOAPService soapService;
    @Bean
    public Endpoint endpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, soapService);
        endpoint.publish("/service");
        return endpoint;
    }

}
