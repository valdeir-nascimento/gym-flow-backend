@org.springframework.modulith.ApplicationModule(
    displayName = "Authentication",
    allowedDependencies = {"shared", "api", "users::spi", "users::events"}
)
package br.com.gym.flow.authentication;
