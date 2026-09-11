package cn.edu.tju.takeout.admin;

import jakarta.validation.constraints.NotBlank;
public record AdminStatusRequest(@NotBlank String status) {}
