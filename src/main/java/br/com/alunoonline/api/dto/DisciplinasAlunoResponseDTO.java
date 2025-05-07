package br.com.alunoonline.api.dto;

import br.com.alunoonline.api.enums.MatriculoAlunoStatusEnum;
import lombok.Data;

@Data
public class DisciplinasAlunoResponseDTO {
    private String nomeDisciplina;
    private String nomeProfessor;
    private Double nota1;
    private Double nota2;
    private Double media;
    private MatriculoAlunoStatusEnum status;
}
