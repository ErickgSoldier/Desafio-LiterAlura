package br.com.alura.literalura.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ConsumoAPI {

    private static final String URL = "https://gutendex.com/books/?search=";

    public String obterDados(String titulo) {
        return new RestTemplate().getForObject(URL + titulo.replace(" ", "%20"), String.class);
    }
}

