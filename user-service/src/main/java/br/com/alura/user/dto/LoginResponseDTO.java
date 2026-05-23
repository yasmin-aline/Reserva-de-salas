package br.com.alura.user.dto;

public record LoginResponseDTO(String token, String tokenType, long expiresIn) {}
