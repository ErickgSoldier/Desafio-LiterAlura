package br.com.alura.literalura.principal;

import br.com.alura.literalura.dto.AutorDTO;
import br.com.alura.literalura.dto.DadosLivroDTO;
import br.com.alura.literalura.model.Autor;
import br.com.alura.literalura.model.Livro;
import br.com.alura.literalura.repository.AutorRepository;
import br.com.alura.literalura.repository.LivroRepository;
import br.com.alura.literalura.service.ConsumoAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

@Component
public class Principal implements CommandLineRunner {

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private ConsumoAPI consumoAPI;

    private final Scanner scanner = new Scanner(System.in);

    @Override
    public void run(String... args) {
        int opcao = -1;

        while (opcao != 0) {
            System.out.println("""
                1 - Buscar livro pelo título
                2 - Listar livros registrados
                3 - Listar autores
                4 - Listar autores vivos em determinado ano
                5 - Listar livros por idioma
                0 - Sair
                """);

            String entrada = scanner.nextLine();
            try {
                opcao = Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                opcao = -1;
            }

            switch (opcao) {
                case 1 -> buscarLivro();
                case 2 -> listarLivros();
                case 3 -> listarAutores();
                case 4 -> listarAutoresPorAno();
                case 5 -> listarLivrosPorIdioma();
            }
        }
    }

    private void buscarLivro() {
        System.out.print("Digite o título: ");
        String titulo = scanner.nextLine();

        try {
            String json = consumoAPI.obterDados(titulo);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode results = root.get("results");

            if (results == null || results.isEmpty()) {
                System.out.println("Nenhum livro encontrado na API Gutendex.");
                return;
            }

            JsonNode livroJson = results.get(0);
            DadosLivroDTO dto = mapper.treeToValue(livroJson, DadosLivroDTO.class);

            if (dto.authors() == null || dto.authors().isEmpty()) {
                System.out.println("Livro encontrado, mas sem autor cadastrado.");
                return;
            }

            AutorDTO autorDTO = dto.authors().get(0);

            Autor autor = autorRepository
                    .findByNomeIgnoreCase(autorDTO.name())
                    .orElseGet(() -> {
                        Autor novo = new Autor();
                        novo.setNome(autorDTO.name());
                        novo.setAnoNascimento(autorDTO.birth_year());
                        novo.setAnoFalecimento(autorDTO.death_year());
                        return autorRepository.save(novo);
                    });

            Livro livro = new Livro();
            livro.setTitulo(dto.title());
            livro.setIdioma(
                    dto.languages() != null && !dto.languages().isEmpty()
                            ? dto.languages().get(0).toUpperCase()
                            : "DESCONHECIDO"
            );
            livro.setDownloads(dto.download_count());
            livro.setAutor(autor);

            livroRepository.save(livro);

            System.out.println("Livro salvo com sucesso!");
            System.out.println("Título: " + livro.getTitulo());
            System.out.println("Autor: " + autor.getNome());
            System.out.println("Idioma: " + livro.getIdioma());
            System.out.println("Downloads: " + livro.getDownloads());

        } catch (Exception e) {
            System.out.println("Erro inesperado ao buscar livro.");
            e.printStackTrace();
        }
    }

    private void listarLivros() {
        livroRepository.findAll()
                .forEach(l -> System.out.println(l.getTitulo() + " - " + l.getAutor().getNome()));
    }

    private void listarAutores() {
        autorRepository.findAll().forEach(a -> {
            System.out.println(a.getNome());
            a.getLivros().forEach(l -> System.out.println("  - " + l.getTitulo()));
        });
    }

    private void listarAutoresPorAno() {
        System.out.print("Digite o ano: ");
        String entrada = scanner.nextLine();

        int ano;
        try {
            ano = Integer.parseInt(entrada);
        } catch (NumberFormatException e) {
            System.out.println("Ano inválido.");
            return;
        }

        autorRepository.autoresVivosNoAno(ano)
                .forEach(a -> System.out.println(a.getNome()));
    }

    private void listarLivrosPorIdioma() {
        System.out.print("Idioma (PT, EN, ES, FR): ");
        String idioma = scanner.nextLine();

        List<Livro> livros = livroRepository.findByIdiomaIgnoreCase(idioma);

        if (livros.isEmpty()) {
            System.out.println("Nenhum livro encontrado nesse idioma.");
        } else {
            livros.forEach(l -> System.out.println(l.getTitulo()));
        }
    }
}
