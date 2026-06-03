# ADR 0001 — Modelo de persistência JPA separado do domínio

- **Status:** Aceito
- **Contexto:** gym-flow-backend (Java 21, Spring Boot 3.5, Spring Modulith, DDD + Arquitetura Hexagonal/Limpa)
- **Aplica-se a:** todos os módulos de negócio (`users`, `exercises`, `trainings`, `history`, …)

## Contexto

Cada módulo segue a arquitetura hexagonal em camadas: `domain` (agregado + value
objects + portas) · `application` (use cases + mappers) · `infrastructure`
(entidades JPA + adapters) · `presentation` (controllers + DTOs).

Na camada de infraestrutura, as entidades JPA seguem **sempre** o mesmo formato.
Exemplos canônicos:

- [`ExerciseJpaEntity`](../../src/main/java/br/com/gym/flow/exercises/infrastructure/ExerciseJpaEntity.java)
- [`BondJpaEntity`](../../src/main/java/br/com/gym/flow/users/infrastructure/BondJpaEntity.java)
- [`TrainingJpaEntity`](../../src/main/java/br/com/gym/flow/trainings/infrastructure/TrainingJpaEntity.java)
- [`WorkoutExecutionJpaEntity`](../../src/main/java/br/com/gym/flow/history/infrastructure/WorkoutExecutionJpaEntity.java)

Este documento registra **por que** esse formato foi escolhido.

## Decisão

Adotar o padrão **_separate persistence model_**: a entidade `@Entity` é um
detalhe de infraestrutura, isolado e "burro", distinto do agregado de domínio.
Um **mapper** na camada de infraestrutura converte entre os dois, e o **adapter**
implementa a porta de repositório definida no `domain`.

```
domain.Exercise            (agregado rico, sem JPA)
   ▲  mapeado por
infrastructure.ExerciseJpaMapper
   ▼
infrastructure.ExerciseJpaEntity   (@Entity, package-private, "burra")
   ▲  usada por
infrastructure.ExerciseRepositoryAdapter  implements  domain.ExerciseRepository
```

## Características do formato e justificativa

### 1. A entidade JPA não é o agregado de domínio

O agregado (`Exercise`, `TeacherStudentBond`) é puro: **não importa nada de
`jakarta.persistence`**. A entidade JPA é uma classe espelho na infraestrutura.

**Por quê:** Clean/Hexagonal Architecture. O domínio não sabe que existe banco.
O agregado protege invariantes por construtores/factories; a entidade JPA precisa
de construtor sem-args e campos mutáveis (exigência do Hibernate), o que poluiria
o domínio. Separando, cada lado faz o que precisa sem contaminar o outro.

**Custo aceito:** o boilerplate de mapeamento. Troca-se esse código por um
domínio 100% livre de ORM.

### 2. Referências a outros agregados por `UUID`, nunca `@ManyToOne`

`studentId`, `instructorId`, `createdBy`, `exerciseId`, `trainingId` são `UUID` —
não associações ORM.

**Por quê:**
- **Fronteira de agregado (DDD):** referencia-se outro agregado *por identidade*,
  não por referência de objeto.
- **Spring Modulith:** um `@ManyToOne` para a entidade `User` (módulo `users`) a
  partir de `Exercise` (módulo `exercises`) quebraria a fronteira do módulo —
  acoplamento de schema entre contextos.
- **Performance (evita N+1 por construção):** o default de `@ManyToOne`/`@OneToOne`
  é **EAGER**, a maior fonte involuntária de N+1 e queries gordas. Sem
  relacionamentos ORM, não existe N+1 acidental nem grafo gordo, e por isso o
  `spring.jpa.open-in-view: false` funciona sem `LazyInitializationException`.

Relações **pai-filho dentro do mesmo agregado** (ex.: `Training` → itens) também
são mapeadas manualmente (duas entidades + duas tabelas, costuradas pelo adapter),
não via `@OneToMany`.

### 3. Enums persistidos como `String`, não `@Enumerated`

`status`, `muscleGroup`, `difficultyLevel` são `String` na entidade; o domínio tem
enums de verdade e o mapper faz `.name()` / `valueOf()`.

**Por quê:**
- `@Enumerated(ORDINAL)` é uma armadilha: reordenar constantes corrompe dados.
- `@Enumerated(STRING)` funciona, mas amarra a coluna à classe do enum de domínio
  na camada de persistência.
- `String` mantém a entidade desacoplada do enum; a **constraint `CHECK` no banco**
  (ex.: `ck_trainings_status`) valida os valores permitidos. O mapper é o único
  ponto de conversão.

Não se perde segurança de tipo: ela existe no domínio (enum) e no banco (CHECK).

### 4. `@Version` apenas na entidade JPA

O token de optimistic lock vive **só** no modelo de persistência; o agregado fica
_version-agnostic_.

**Por quê:**
- Optimistic locking é detalhe de infraestrutura — o domínio não carrega `version`.
- `version == null` deixa o Spring Data decidir **`persist` (insert) vs `merge`
  (update)** sozinho, sem `exists` separado (new-entity detection).

> Entidades **imutáveis** (ex.: `WorkoutExecution`, insert-only) **não** têm
> `@Version` — optimistic locking não se aplica a algo que nunca é atualizado.

### 5. Classe e campos package-private, acesso por campo, construtor sem-args, sem equals/hashCode

**Por quê cada um:**
- **package-private:** a entidade nunca escapa do pacote `infrastructure`. Só o
  mapper (mesmo pacote) toca os campos. Nada externo depende dela → impossível
  vazar entidade num endpoint REST.
- **acesso por campo (sem getters/setters):** o Hibernate usa field access e o
  mapper lê os campos direto. Sem API pública, não há motivo para acessores.
- **sem `@Data`/equals/hashCode:** a entidade nunca entra num `Set` nem é chave de
  mapa; a identidade por id mora no `AggregateRoot` do domínio. Evita o bug
  clássico do Lombok `@Data` em entidade (equals que quebra coleção, hashCode que
  muda quando o banco atribui o id).
- **construtor sem-args:** exigência da spec JPA (instanciação por reflection).

### 6. `nullable` / `length` explícitos no `@Column`

Não é para gerar DDL — o **Flyway** é dono do schema (`spring.jpa.hibernate.ddl-auto:
validate`). O metadado serve para o `validate` no startup **cruzar a entidade
contra a tabela** criada pela migration e documentar o schema no código.

## Configuração que sustenta a decisão

`application.yaml`:

```yaml
spring:
  jpa:
    open-in-view: false          # sem OSIV; tudo é buscado dentro da transação
    hibernate:
      ddl-auto: validate         # schema é responsabilidade do Flyway
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
```

## Consequências

**Positivas**
- Domínio puro, testável sem banco e sem Spring.
- Fronteiras de módulo (Spring Modulith) intactas — sem ORM atravessando contextos.
- Zero armadilha de fetch (EAGER/N+1/`LazyInitializationException`) por construção.
- Entidade impossível de vazar para o contrato HTTP.
- Schema versionado e validado (Flyway + `ddl-auto: validate`).

**Negativas / custos**
- Boilerplate de mapeamento (mappers) e duas representações do mesmo dado.
- Relações pai-filho do mesmo agregado são costuradas à mão no adapter
  (insert/replace de filhos), em vez de cascade automático.
- Joins entre agregados, quando necessários para leitura, são feitos por id em
  lote (ex.: `findByTrainingIdIn...`) — não há navegação ORM.

## Alternativas consideradas e rejeitadas

- **Anotar o agregado de domínio diretamente com JPA.** Menos código, mas acopla o
  domínio ao Hibernate, exige construtor sem-args/campos mutáveis no agregado e
  reintroduz as armadilhas de fetch. Rejeitado.
- **`@ManyToOne`/`@OneToMany` entre agregados.** Quebra fronteira de módulo e traz
  EAGER/N+1. Rejeitado em favor de referência por `UUID`.
- **`@Enumerated(STRING)`.** Aceitável, mas amarra a coluna ao enum de domínio.
  Preferiu-se `String` + `CHECK`.

## Referências

- Princípios de JPA/Spring Data adotados no projeto (fetch LAZY, `open-in-view:
  false`, não expor entidade, paginação, equals/hashCode por id).
- `ArchitectureTests` (`ApplicationModules.of(...).verify()`) valida as fronteiras
  de módulo que esta decisão preserva.
