@org.springframework.modulith.ApplicationModule(
    displayName = "API Layer",
    type = org.springframework.modulith.ApplicationModule.Type.OPEN,
    allowedDependencies = {"shared", "users::spi", "users::events", "authentication::spi"}
)
package br.com.gym.flow.api;
