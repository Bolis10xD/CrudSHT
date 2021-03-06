package com.uatx.springboot.app.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.uatx.springboot.app.models.entity.Producto;
import com.uatx.springboot.app.models.service.IProductoService;
import com.uatx.springboot.app.models.service.IUploadFileService;
import com.uatx.springboot.app.util.paginator.PageRender;

@Controller
@SessionAttributes("producto")
public class ProductoController {

	@Autowired
	private IProductoService productoService;

	@Autowired
	private IUploadFileService uploadFileService;

	@GetMapping(value = "/uploads/{filename:.+}")
	public ResponseEntity<Resource> verFoto(@PathVariable String filename) {

		Resource recurso = null;

		try {
			recurso = uploadFileService.load(filename);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"")
				.body(recurso);
	}

	@GetMapping(value = "/ver/{id}")
	public String ver(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {

		Producto producto = productoService.findOne(id);
		if (producto == null) {
			flash.addFlashAttribute("error", "El Producto no existe en la base de datos");
			return "redirect:/listar";
		}

		model.put("producto", producto);
		model.put("titulo", "Detalle del Producto: " + producto.getNombre());
		return "ver";
	}

	@RequestMapping(value = {"/listar", "/buscartodos"}, method = RequestMethod.GET)
	public String listar(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {

		Pageable pageRequest = PageRequest.of(page, 4);

		Page<Producto> producto = productoService.findAll(pageRequest);

		PageRender<Producto> pageRender = new PageRender<Producto>("/listar", producto);
		model.addAttribute("titulo", "Listado de productos");
		model.addAttribute("productos", producto);
		model.addAttribute("page", pageRender);
		return "listar";
	}

	@RequestMapping(value = {"/form", "/productos/page/nuevo-producto"})
	public String crear(Map<String, Object> model) {

		Producto producto = new Producto();
		producto.setStock(0.0);
		producto.setCreateAt(new Date());
		model.put("producto", producto);
		model.put("titulo", "Formulario de Producto");
		return "form";
	}

	@RequestMapping(value = "/form/{id}")
	public String editar(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {

		Producto producto = null;

		if (id > 0) {
			producto = productoService.findOne(id);
			if (producto == null) {
				flash.addFlashAttribute("error", "El ID del producto no existe en la BBDD!");
				return "redirect:/listar";
			}
		} else {
			flash.addFlashAttribute("error", "El ID del producto no puede ser cero!");
			return "redirect:/listar";
		}
		model.put("producto", producto);
		model.put("titulo", "Editar Cliente");
		return "form";
	}

	@RequestMapping(value = "/form", method = RequestMethod.POST)
	public String guardar(@Valid Producto producto, BindingResult result, Model model,
						  @RequestParam("file") MultipartFile foto, RedirectAttributes flash, SessionStatus status) {

		if (result.hasErrors()) {
			model.addAttribute("titulo", "Formulario de producto");
			return "form";
		}

		if (!foto.isEmpty()) {

			if (producto.getId() != null && producto.getId() > 0 && producto.getFoto() != null
					&& producto.getFoto().length() > 0) {

				uploadFileService.delete(producto.getFoto());
			}

			String uniqueFilename = null;
			try {
				uniqueFilename = uploadFileService.copy(foto);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			flash.addFlashAttribute("info", "Has subido correctamente '" + uniqueFilename + "'");

			producto.setFoto(uniqueFilename);
		}

		String mensajeFlash = (producto.getId() != null) ? "Producto editado con éxito!" : "Cliente creado con éxito!";


		if(producto.getFoto() == null){
			producto.setFoto("");
			productoService.save(producto);
		}else{
			productoService.save(producto);
		}
		status.setComplete();
		flash.addFlashAttribute("success", mensajeFlash);
		return "redirect:listar";
	}

	@RequestMapping(value = "/eliminar/{id}")
	public String eliminar(@PathVariable(value = "id") Long id, RedirectAttributes flash) {

		if (id > 0) {
			Producto producto = productoService.findOne(id);

			productoService.delete(id);
			flash.addFlashAttribute("success", "Producto eliminado con éxito!");

			if (uploadFileService.delete(producto.getFoto())) {
				flash.addFlashAttribute("info", "Foto " + producto.getFoto() + " eliminada con exito!");
			}

		}
		return "redirect:/listar";
	}
}