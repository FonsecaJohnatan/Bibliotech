package br.edu.bibliotech.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.persistence.EntityNotFoundException;

@ControllerAdvice(basePackageClasses = HomeController.class)
public class MvcExceptionHandler {

	@ExceptionHandler(EntityNotFoundException.class)
	public ModelAndView tratarEntidadeNaoEncontrada(EntityNotFoundException exception) {
		ModelAndView pagina = new ModelAndView("erro");
		pagina.setStatus(HttpStatus.NOT_FOUND);
		pagina.addObject("mensagem", exception.getMessage());
		return pagina;
	}
}
