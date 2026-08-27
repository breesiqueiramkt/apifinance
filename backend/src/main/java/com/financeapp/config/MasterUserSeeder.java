package com.financeapp.config;

import com.financeapp.entity.User;
import com.financeapp.repository.UserRepository;
import com.financeapp.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Este app não tem cadastro público - existe exatamente um usuário, criado
 * automaticamente aqui na subida da aplicação a partir de três variáveis de
 * ambiente (MASTER_USER_NAME, MASTER_USER_EMAIL, MASTER_USER_PASSWORD).
 *
 * É por isso que você e sua esposa sempre vão logar com o MESMO e-mail e
 * senha, vendo os mesmos dados: não existem duas contas para "juntar", só
 * existe uma.
 *
 * Idempotente e seguro para rodar em todo restart: se já existir QUALQUER
 * usuário no banco, este seeder não faz nada (nunca sobrescreve a senha de
 * um usuário existente). Ele só cria o usuário master na primeiríssima
 * subida, com o banco vazio.
 */
@Component
@Slf4j
@Order(0)
public class MasterUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryService categoryService;

    private final String masterName;
    private final String masterEmail;
    private final String masterPassword;

    public MasterUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CategoryService categoryService,
            @Value("${app.master-user.name:}") String masterName,
            @Value("${app.master-user.email:}") String masterEmail,
            @Value("${app.master-user.password:}") String masterPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryService = categoryService;
        this.masterName = masterName;
        this.masterEmail = masterEmail;
        this.masterPassword = masterPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            // já existe usuário (o master já foi criado antes) - nunca sobrescreve
            return;
        }

        if (isBlank(masterEmail) || isBlank(masterPassword)) {
            log.warn("Nenhum usuário existe ainda e MASTER_USER_EMAIL / MASTER_USER_PASSWORD não "
                    + "foram configurados. Defina essas variáveis de ambiente e reinicie a aplicação "
                    + "para criar o usuário master - sem isso, ninguém consegue fazer login.");
            return;
        }

        User master = User.builder()
                .name(isBlank(masterName) ? "Família" : masterName.trim())
                .email(masterEmail.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(masterPassword))
                .build();

        userRepository.save(master);

        // mesmo seed idempotente que o antigo fluxo de cadastro fazia,
        // pra já existirem categorias padrão prontas pro primeiro uso
        categoryService.ensureDefaultCategoriesExist();

        log.info("Usuário master criado com sucesso ({}). Use este e-mail e a senha definida em "
                + "MASTER_USER_PASSWORD para entrar - o mesmo login para você e sua esposa.",
                master.getEmail());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
