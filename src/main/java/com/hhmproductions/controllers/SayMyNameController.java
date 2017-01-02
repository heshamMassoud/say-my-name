package com.hhmproductions.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by heshammassoud on 30/12/16.
 */
@RestController
@RequestMapping("/say")
public class SayMyNameController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/{name}")
    public String index(@PathVariable String name) throws UnknownHostException {
        String hostName = InetAddress.getLocalHost().getHostName();
        String hostAddress = InetAddress.getLocalHost().getHostAddress();

        return "Hi " + name + "! :) It's " + hostName + ":" + hostAddress;
    }

    @RequestMapping("/**")
    public String getAll() throws NoSuchMethodException {
        logger.error("This endpoint is not found!");
        if (true)
            throw new NoSuchMethodException();
        return "Get all resources";
    }

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    void handleBadRequests(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), "Please enter the correct value!");
    }

    @ExceptionHandler({NoSuchMethodException.class})
    void handleNotFoundRequests(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value(), "This page is not available!");
    }

}
