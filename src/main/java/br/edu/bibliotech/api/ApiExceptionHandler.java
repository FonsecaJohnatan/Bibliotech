package br.edu.bibliotech.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice(basePackageClasses = AutorRest.class)
public class ApiExceptionHandler {

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ErroResposta> tratarEntidadeNaoEncontrada(EntityNotFoundException exception) {
		ErroResposta erro = new ErroResposta();
		erro.setMensagem(exception.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErroResposta> tratarArgumentoInvalido(IllegalArgumentException exception) {
		ErroResposta erro = new ErroResposta();
		erro.setMensagem(exception.getMessage());
		return ResponseEntity.badRequest().body(erro);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErroResposta> tratarEstadoInvalido(IllegalStateException exception) {
		ErroResposta erro = new ErroResposta();
		erro.setMensagem(exception.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
	}

	public static class ErroResposta {

		private String mensagem;

		public String getMensagem() {
			return mensagem;
		}

		public void setMensagem(String mensagem) {
			this.mensagem = mensagem;
		}
	}
}
