package br.com.alunoonline.api.service;

import br.com.alunoonline.api.dto.AtualizarNotasRequestDTO;
import br.com.alunoonline.api.dto.DisciplinasAlunoResponseDTO;
import br.com.alunoonline.api.dto.HistoricoAlunoResponseDTO;
import br.com.alunoonline.api.enums.MatriculoAlunoStatusEnum;
import br.com.alunoonline.api.model.MatriculaAluno;
import br.com.alunoonline.api.repository.MatriculaAlunoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MatriculaAlunoService {
    public static final double MEDIA_PARA_APROVACAO = 7.0;
    public static final Integer QNT_NOTAS = 2;

    @Autowired
    MatriculaAlunoRepository matriculaAlunoRepository;

    public void criarMatricula(MatriculaAluno matriculaAlunoId) {
        matriculaAlunoId.setStatus(MatriculoAlunoStatusEnum.MATRICULADO);
        matriculaAlunoRepository.save(matriculaAlunoId);
    }

    public void trancarMatricula(Long matriculaAlunoId) {
        MatriculaAluno matriculaAluno = matriculaAlunoRepository.findById(matriculaAlunoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));


        if (matriculaAluno.getStatus().equals(MatriculoAlunoStatusEnum.MATRICULADO)) {
            matriculaAluno.setStatus((MatriculoAlunoStatusEnum.TRANCADO));
            matriculaAlunoRepository.saveAndFlush(matriculaAluno);
        } else {
            throw new ResponseStatusException((HttpStatus.BAD_REQUEST), "Só é possível trancar o curso com o status MATRICULADO");

        }
        matriculaAluno.setStatus(MatriculoAlunoStatusEnum.TRANCADO);
        matriculaAlunoRepository.save(matriculaAluno);
    }

    public void atualizarNotas(Long matriculaAlunoId, AtualizarNotasRequestDTO request) {
        MatriculaAluno matriculaAluno = buscarMatriculaOuLancarExcecao(matriculaAlunoId);

        if (request.getNota1() != null) {
            matriculaAluno.setNota1(request.getNota1());
        }

        if (request.getNota2() != null) {
            matriculaAluno.setNota2(request.getNota2());
        }

        calculaMediaEModificaStatus(matriculaAluno);
        matriculaAlunoRepository.save(matriculaAluno);
    }

    public HistoricoAlunoResponseDTO emitirHistorico(Long alunoId) {
        List<MatriculaAluno> matriculaAlunos = matriculaAlunoRepository.findByAlunoIdAluno(alunoId);
        if (matriculaAlunos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esse aluno não possui matriculas!");
        }
        HistoricoAlunoResponseDTO historicoAluno = new HistoricoAlunoResponseDTO();
        historicoAluno.setNomeAluno(matriculaAlunos.get(0).getAluno().getNome());
        historicoAluno.setEmailAluno(matriculaAlunos.get(0).getAluno().getEmail());
        historicoAluno.setCpfAluno(matriculaAlunos.get(0).getAluno().getCpf());
        List<DisciplinasAlunoResponseDTO> disciplinas = new ArrayList<>(matriculaAlunos
                .stream()
                .map(this::mapearParaDisciplinasAlunoResponseDTO)
                .toList());
        historicoAluno.setDisciplinasAlunoResponsesDTO(disciplinas);
        return historicoAluno;
    }

    private MatriculaAluno buscarMatriculaOuLancarExcecao(Long matriculaAlunoId){
        return matriculaAlunoRepository.findById(matriculaAlunoId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Matricula Aluno não encontrada!"));
    }

    private void calculaMediaEModificaStatus(MatriculaAluno matriculaAluno) {
        Double nota1 = matriculaAluno.getNota1();
        Double nota2 = matriculaAluno.getNota2();

        if (nota1 != null && nota2 != null) {
            Double media = (nota1 + nota2) / QNT_NOTAS;
            matriculaAluno.setStatus(
                            media >= MEDIA_PARA_APROVACAO ?
                            MatriculoAlunoStatusEnum.APROVADO :
                            MatriculoAlunoStatusEnum.REPROVADO);
        }
    }

    private Double calcularMedia(Double nota1, Double nota2){
        return (nota1 != null && nota2 != null) ? (nota1 + nota2) / QNT_NOTAS : null;
    }

    private DisciplinasAlunoResponseDTO mapearParaDisciplinasAlunoResponseDTO(MatriculaAluno matriculaAluno){
        DisciplinasAlunoResponseDTO response = new DisciplinasAlunoResponseDTO();
        response.setNomeDisciplina(matriculaAluno.getDisciplina().getNome());
        response.setNomeProfessor(matriculaAluno.getDisciplina().getProfessor().getNome());
        response.setNota1(matriculaAluno.getNota1());
        response.setNota2(matriculaAluno.getNota2());
        response.setMedia(calcularMedia(matriculaAluno.getNota1(),matriculaAluno.getNota2()));
        response.setStatus(matriculaAluno.getStatus());
        return response;
    }
}
