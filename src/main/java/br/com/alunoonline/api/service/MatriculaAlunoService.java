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

@Service
public class MatriculaAlunoService {
    public static final double MEDIA_PARA_APROVACAO = 7.0;

    @Autowired
    MatriculaAlunoRepository matriculaAlunoRepository;

    public void criarMatricula(MatriculaAluno matriculaAluno){
        matriculaAluno.setStatus(MatriculoAlunoStatusEnum.MATRICULADO);
        matriculaAlunoRepository.save(matriculaAluno);
    }

    public void trancarMatricula(Long matriculaAlunoId){
        MatriculaAluno matriculaAluno = matriculaAlunoRepository.findById(matriculaAlunoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));


        if(matriculaAluno.getStatus().equals(MatriculoAlunoStatusEnum.MATRICULADO)){
            matriculaAluno.setStatus((MatriculoAlunoStatusEnum.TRANCADO));
            matriculaAlunoRepository.saveAndFlush(matriculaAluno);
        } else{
            throw new ResponseStatusException((HttpStatus.BAD_REQUEST), "Só é possível trancar o curso com o status MATRICULADO");

        }




//        if (!MatriculoAlunoStatusEnum.MATRICULADO.equals(matriculaAluno.getStatus())){
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                    "Só é possível trancar uma matricula com o status MATRICULADO");
//        }

        matriculaAluno.setStatus(MatriculoAlunoStatusEnum.TRANCADO);
        matriculaAlunoRepository.save(matriculaAluno);
    }

    public void atualizarNotas(AtualizarNotasRequestDTO request, Long idMatricula){
        MatriculaAluno matriculaAluno = matriculaAlunoRepository.findById(idMatricula).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Matricula Aluno não encontrada!"));

        if (request.getNota1() != null) {
            matriculaAluno.setNota1(request.getNota1());
        }

        if (request.getNota2() != null) {
            matriculaAluno.setNota2(request.getNota2());
        }

        calculaMedia(matriculaAluno);
        matriculaAlunoRepository.save(matriculaAluno);
    }

    private void calculaMedia(MatriculaAluno matriculaAluno) {
        Double nota1 = matriculaAluno.getNota1();
        Double nota2 = matriculaAluno.getNota2();

        if (nota1 != null && nota2 != null) {
            Double media = (nota1 + nota2) / 2;
            matriculaAluno.setStatus(media >= MEDIA_PARA_APROVACAO ? MatriculoAlunoStatusEnum.APROVADO : MatriculoAlunoStatusEnum.REPROVADO);

        }
    }

    public HistoricoAlunoResponseDTO emitirHistorico(Long idAluno){
        List<MatriculaAluno> matriculasDoAluno = matriculaAlunoRepository.findByAlunoIdAluno(idAluno);

        if (matriculasDoAluno.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Esse aluno não possui matriculas");
        }

        HistoricoAlunoResponseDTO historicoAluno = new HistoricoAlunoResponseDTO();
        historicoAluno.setNomeAluno(matriculasDoAluno.get(0).getAluno().getNome());
        historicoAluno.setCpfAluno(matriculasDoAluno.get(0).getAluno().getCpf());
        historicoAluno.setEmailAluno(matriculasDoAluno.get(0).getAluno().getEmail());

        List<DisciplinasAlunoResponseDTO> displinasList = new ArrayList<>();

        for (MatriculaAluno matriculaAluno : matriculasDoAluno) {
            DisciplinasAlunoResponseDTO disciplinasAlunoResponse = new DisciplinasAlunoResponseDTO();
            disciplinasAlunoResponse.setNomeDisciplina(matriculaAluno.getDisciplina().getNome());
            disciplinasAlunoResponse.setNomeProfessor(matriculaAluno.getDisciplina().getProfessor().getNome());
            disciplinasAlunoResponse.setNota1(matriculaAluno.getNota1());
            disciplinasAlunoResponse.setNota2(matriculaAluno.getNota2());

            if (matriculaAluno.getNota1() != null && matriculaAluno.getNota2() != null) {
                disciplinasAlunoResponse.setMedia((matriculaAluno.getNota1() + matriculaAluno.getNota2()) / 2.0);
            } else {
                disciplinasAlunoResponse.setMedia(null);
            }

            disciplinasAlunoResponse.setStatus(matriculaAluno.getStatus());

            displinasList.add(disciplinasAlunoResponse);
        }

        historicoAluno.setDisciplinasAlunoResponsesDTO(displinasList);

        return historicoAluno;
    }
}
