package br.com.alura.user.dto;

public record TwoFactorRequestDTO(String preAuthToken, String codigoOtp) {}
