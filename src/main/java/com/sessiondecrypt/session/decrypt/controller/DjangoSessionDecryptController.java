package com.sessiondecrypt.session.decrypt.controller;


import com.sessiondecrypt.session.decrypt.service.DjangoSessionDecryptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

@RestController
public class DjangoSessionDecryptController {
    @Autowired
    DjangoSessionDecryptService djangoSessionDecryptService;

    public static byte[] decompress(byte[] data) throws DataFormatException, IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }

    public static String truncateAfterColon(String input) {
        int index = input.indexOf(':');
        if (index != -1) {
            return input.substring(0, index);
        }
        return input;
    }

    public static String removeLeadingPeriod(String input) {
        if (input != null && !input.isEmpty() && input.charAt(0) == '.') {
            return input.substring(1);
        }
        return input;
    }

    @GetMapping("/session-decrypt")
    public ResponseEntity<String> demo(@RequestParam(name = "session-id") String urlSafeEncoded) {

        djangoSessionDecryptService.decryptSession(urlSafeEncoded);

        // Replace URL-safe characters
        String removedColon = truncateAfterColon(urlSafeEncoded);
        String removePeriodIfExists = removeLeadingPeriod(removedColon);
        System.out.println("removed after colon= " + removedColon);
        System.out.println(". removed if existed = " + removePeriodIfExists);
        String standardEncoded = removePeriodIfExists.replace('-', '+').replace('_', '/');

        String resultSout ="";

        // Check if the encoded string is valid base64 and attempt to decode and decompress
        try {
            byte[] decodedData = Base64.getDecoder().decode(standardEncoded);
            System.out.println("Valid base64 encoding");

            try {
                byte[] decompressedData = decompress(decodedData);
                String result = new String(decompressedData, StandardCharsets.UTF_8);
                System.out.println("Decoded and decompressed session data:");
                resultSout = result;
                System.out.println(result);


            } catch (DataFormatException | IOException e) {
                System.err.println("Failed to decode and decompress session data: " + e.getMessage());
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Invalid base64 encoding: " + e.getMessage());
        }


        return ResponseEntity.ok("final converted session data= \n" +
                standardEncoded + "\nlength= " +
                standardEncoded.length() + "\n" + resultSout



        );
    }
}
