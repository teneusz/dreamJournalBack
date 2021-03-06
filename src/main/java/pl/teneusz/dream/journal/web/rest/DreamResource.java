package pl.teneusz.dream.journal.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.jhipster.web.util.ResponseUtil;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.teneusz.dream.journal.domain.Dream;
import pl.teneusz.dream.journal.domain.DreamHelper;
import pl.teneusz.dream.journal.domain.Tag;
import pl.teneusz.dream.journal.domain.User;
import pl.teneusz.dream.journal.domain.enumeration.GenderEnum;
import pl.teneusz.dream.journal.repository.DreamRepository;
import pl.teneusz.dream.journal.repository.UserRepository;
import pl.teneusz.dream.journal.repository.search.DreamSearchRepository;
import pl.teneusz.dream.journal.security.SecurityUtils;
import pl.teneusz.dream.journal.service.dto.DiagramDto;
import pl.teneusz.dream.journal.web.rest.util.HeaderUtil;
import pl.teneusz.dream.journal.web.rest.util.PaginationUtil;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

/**
 * REST controller for managing Dream.
 */
@RestController
@RequestMapping("/api")
public class DreamResource {

    private final Logger log = LoggerFactory.getLogger(DreamResource.class);

    private static final String ENTITY_NAME = "dream";

    private final DreamRepository dreamRepository;

    private final DreamSearchRepository dreamSearchRepository;
    private final UserRepository userRepository;

    public DreamResource(DreamRepository dreamRepository, DreamSearchRepository dreamSearchRepository, UserRepository userRepository) {
        this.dreamRepository = dreamRepository;
        this.dreamSearchRepository = dreamSearchRepository;
        this.userRepository = userRepository;
    }

    /**
     * POST  /dreams : Create a new dream.
     *
     * @param dream the dream to create
     * @return the ResponseEntity with status 201 (Created) and with body the new dream, or with status 400 (Bad Request) if the dream has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/dreams")
    @Timed
    public ResponseEntity<Dream> createDream(@Valid @RequestBody Dream dream) throws URISyntaxException {
        log.debug("REST request to save Dream : {}", dream);
        if(dream.getTags() != null && !dream.isIsAdult()){
            dream.setIsAdult(!dream.getTags().stream().filter(t->t.isIsAdult()).collect(Collectors.toList()).isEmpty());
        }
        if (dream.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new dream cannot already have an ID")).body(null);
        }
        dream.setUser(userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).get());
        Dream result = dreamRepository.save(dream);

        dreamSearchRepository.save(result);
        return ResponseEntity.created(new URI("/api/dreams/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /dreams : Updates an existing dream.
     *
     * @param dream the dream to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated dream,
     * or with status 400 (Bad Request) if the dream is not valid,
     * or with status 500 (Internal Server Error) if the dream couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/dreams")
    @Timed
    public ResponseEntity<Dream> updateDream(@Valid @RequestBody Dream dream) throws URISyntaxException {
        log.debug("REST request to update Dream : {}", dream);
        if (dream.getId() == null) {
            return createDream(dream);
        }
        Dream result = dreamRepository.save(dream);
        dreamSearchRepository.save(result);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, dream.getId().toString()))
            .body(result);
    }

    /**
     * GET  /dreams : get all the dreams.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of dreams in body
     */
    @GetMapping("/dreams")
    @Timed
    public ResponseEntity<List<Dream>> getAllDreams(@ApiParam Pageable pageable, @RequestParam(value = "birthDate", required = false) Date birthDate) {
        log.debug("REST request to get a page of Dreams");
        Date bDate = birthDate!=null?birthDate:new Date();
        Optional<User> user = userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin());
        if(user.isPresent() && birthDate == null){
            bDate = user.get().getUserDetails() != null?Date.from(user.get().getUserDetails().getBirthDate().toInstant()):new Date();
        }
        Page<Dream> page = dreamRepository.getAllDreams(pageable, bDate);

        List<Long> ids = Lists.newArrayList();
        page.getContent().stream().forEach(dream -> ids.add(dream.getId()));
        List<DreamHelper> helpers = dreamRepository.getAdditionalInfo(ids);
        if(!org.springframework.util.CollectionUtils.isEmpty(helpers)) {
            Map<Long, DreamHelper> map = Maps.newHashMap();
            helpers.stream().forEach(dreamHelper -> map.put(dreamHelper.getDreamId(), dreamHelper));
            page.getContent().stream().forEach(dream -> {
                Long id = dream.getId();
                if (map.get(id) != null) {
                    dream.setCommentCount(map.get(id).getCommentCount().intValue());
                }
            });
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/dreams");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /dreams/:id : get the "id" dream.
     *
     * @param id the id of the dream to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the dream, or with status 404 (Not Found)
     */
    @GetMapping("/dreams/{id}")
    @Timed
    public ResponseEntity<Dream> getDream(@PathVariable Long id) {
        log.debug("REST request to get Dream : {}", id);
        Dream dream = dreamRepository.findOneWithEagerRelationships(id);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(dream));
    }

    @GetMapping("/dreamsByTag/{tag}")
    @Timed
    public ResponseEntity<List<Dream>> getDreamsByTag(@ApiParam Pageable pageable, @PathVariable String tag) {
        log.debug("REST request to get Dreams by tsg : {}", tag);
        Page<Dream> page = dreamRepository.getDreamsByTag(pageable,tag);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/dreamsByTag");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/myDreams")
    @Timed
    public ResponseEntity<List<Dream>> getMyDreams(@ApiParam Pageable pageable) {
        log.debug("REST request to get logged user dreams");
        Page<Dream> page = dreamRepository.findByUserIsCurrentUser(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/myDreams");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/diagram/birthDateBetweenYear")
    @Timed
    public ResponseEntity<List<DiagramDto>> getDiagramData(@RequestParam(value = "down", defaultValue = "1900") Integer down, @RequestParam(value = "up", defaultValue = "2017") Integer up) {
        log.debug("REST get diagram data down = " + down + "; up = " + up);
        List<DiagramDto> diagramDtos = dreamRepository.getDiagramScoreByBirthDate(down, up);
        return new ResponseEntity<>(diagramDtos, HttpStatus.OK);
    }

    /**
     * DELETE  /dreams/:id : delete the "id" dream.
     *
     * @param id the id of the dream to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/dreams/{id}")
    @Timed
    public ResponseEntity<Void> deleteDream(@PathVariable Long id) {
        log.debug("REST request to delete Dream : {}", id);
        dreamRepository.delete(id);
        dreamSearchRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/dreams?query=:query : search for the dream corresponding
     * to the query.
     *
     * @param query    the query of the dream search
     * @param pageable the pagination information
     * @return the result of the search
     */
    @GetMapping("/_search/dreams")
    @Timed
    public ResponseEntity<List<Dream>> searchDreams(@RequestParam String query, @ApiParam Pageable pageable) {
        log.debug("REST request to search for a page of Dreams for query {}", query);
        Page<Dream> page = dreamSearchRepository.search(queryStringQuery(query), pageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_search/dreams");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/dreamsByUserId/{id}")
    public ResponseEntity<List<Dream>> getDreamsByUserId(@PathVariable Long id, @ApiParam Pageable pageable) {
        log.debug("REST request to get logged user dreams");
        Page<Dream> page = dreamRepository.findDreamsByUserId(id, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/dreamsByUserId");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/tagsCountBetweenYears")
    @Timed
    public List<DiagramDto> getTags(@RequestParam(value = "down", defaultValue = "1900") Long down, @RequestParam(value = "up", defaultValue = "2017") Long up){
        List<Dream> dreams = dreamRepository.findHowManyDreamsWithTagBetweenYear(down.intValue(), up.intValue());
        Map<String,Integer> tagMap = new HashMap<>();
        for(Dream dream: dreams)
        {
            for(Tag tag: dream.getTags()) {
                if (tagMap.get(tag.getName()) == null) {
                    tagMap.put(tag.getName(), 0);
                }
                tagMap.put(tag.getName(), tagMap.get(tag.getName()) + 1);
            }
        }

        return tagMap.entrySet().stream().map(e -> new DiagramDto(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    @GetMapping("/dreamCountByGender/{gender}")
    public List<DiagramDto> foo(@PathVariable(name = "gender") GenderEnum genderEnum, @RequestParam(value = "createFrom", defaultValue = "1970") Integer createDown, @RequestParam(value = "createTo", defaultValue = "2018") Integer createTo,
                                @RequestParam(value = "birthYearFrom", defaultValue = "1970") Integer birthDown, @RequestParam(value = "birthYearTo", defaultValue = "2020") Integer birthTo){
        return dreamRepository.foo(genderEnum,createDown,createTo,birthDown,birthTo);
    }
}
