package br.ufma.nti.forum.api.resource;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.ufma.nti.forum.api.event.RecursoCriadoEvent;
import br.ufma.nti.forum.api.exceptionhandler.UfmaForumExceptionHandler.Erro;
import br.ufma.nti.forum.api.model.Mensagem;
import br.ufma.nti.forum.api.repository.MensagemRepository;
import br.ufma.nti.forum.api.repository.filter.MensagemFilter;
import br.ufma.nti.forum.api.repository.projection.ResumoMensagem;
import br.ufma.nti.forum.api.service.MensagemService;
import br.ufma.nti.forum.api.service.exception.TopicoInexistenteOuInativoException;
import net.sf.jasperreports.engine.JRException;

@RestController
@RequestMapping("/mensagens")
public class MensagemResource {
	
	
	
	@Autowired
	private MensagemRepository mensagemRepository;
	
	
	@Autowired
	ApplicationEventPublisher publisher;
	
	@Autowired
	MensagemService mensagemService;
	
	@Autowired
	private MessageSource messageSource;
	
	@GetMapping
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_MENSAGEM') and #oauth2.hasScope('read')")
	public Page<Mensagem> pesquisar(MensagemFilter mensagemFilter,Pageable pageable){
		return mensagemRepository.filtrar(mensagemFilter,pageable);
	}
	
	@GetMapping(params="resumo")
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_MENSAGEM') and #oauth2.hasScope('read')")
	public Page<ResumoMensagem> resumir(MensagemFilter mensagemFilter,Pageable pageable){
		return mensagemRepository.resumir(mensagemFilter,pageable);
	}
	
	@GetMapping("/relatorios/resumo")
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_MENSAGEM') and #oauth2.hasScope('read')")
	public ResponseEntity<byte[]> relatorioResumido(MensagemFilter mensagemFilter,Pageable pageable) throws JRException{
		byte[] relatorio = mensagemService.relatorioResumido(mensagemFilter, pageable);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE).body(relatorio);
	}
	
	
	@GetMapping("/{codigo}")
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_MENSAGEM') and #oauth2.hasScope('read')")
	public ResponseEntity<Mensagem> buscarPeloCodigo(@PathVariable Long codigo) {
		 Mensagem mensagem = mensagemRepository.findOne(codigo);
		 return mensagem != null ? ResponseEntity.ok(mensagem) : ResponseEntity.notFound().build();
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('ROLE_CADASTRAR_MENSAGEM') and #oauth2.hasScope('write')")
	public ResponseEntity<Mensagem> criar(@Valid @RequestBody Mensagem mensagem, HttpServletResponse response) {
		Mensagem mensagemSalva = mensagemService.criar(mensagem);
		  
		publisher.publishEvent(new RecursoCriadoEvent(this, response, mensagemSalva.getCodigo()));
		
		return ResponseEntity.status(HttpStatus.CREATED).body(mensagemSalva);
		
	}
	
	@PutMapping("/{codigo}")
	@PreAuthorize("hasAuthority('ROLE_CADASTRAR_MENSAGEM') and #oauth2.hasScope('write')")
	public ResponseEntity<Mensagem> atualizar(@PathVariable Long codigo,@Valid @RequestBody Mensagem mensagem){
		Mensagem mensagemSalva = mensagemService.atualizar(codigo, mensagem);
		return ResponseEntity.ok(mensagemSalva);
	}
	
	@ExceptionHandler({TopicoInexistenteOuInativoException.class})
	public ResponseEntity<Object> handleTopicoInexistenteOuInativoException(TopicoInexistenteOuInativoException ex){
		String mensagemUsuario = messageSource.getMessage("topico.inexistente-ou-inativo",null, LocaleContextHolder.getLocale());
		String mensagemDesenvolvedor = ex.toString();
		List<Erro> erros = Arrays.asList(new Erro(mensagemUsuario, mensagemDesenvolvedor));
		return ResponseEntity.badRequest().body(erros);
	}
	
	

	@DeleteMapping("/{codigo}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('ROLE_REMOVER_MENSAGEM') and #oauth2.hasScope('write')")
	public void remover(@PathVariable Long codigo) {
		mensagemRepository.delete(codigo);
	}

}
	
	
	


