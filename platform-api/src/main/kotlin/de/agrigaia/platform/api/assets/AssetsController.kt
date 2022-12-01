package de.agrigaia.platform.api.assets


import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.integration.assets.AssetsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/assets")
class AssetsController @Autowired constructor(
    private val assetsService: AssetsService
) : BaseController() {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCoopSpace(@RequestBody test: String) {
        //TODO maybe mapping Body of Asset (or files -> policy/asset/catalog.json,)

        this.assetsService.publishAssets();
        // TODO implement real business logic
    }

}

