package chess

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/*
 * I'm already paying for the CPU time whether I'm making requests or not, so there's not a big penalty to someone else
 * making requests. But I might as well reduce the attack surface of the application. Wiring up spring security with
 * a real login system seems like overkill, so I'm just going to have a fake token in a prop file for tests. For the
 * real deployment the token is going to be passed to the application as an environment variable from the ECS task definition.
 */
@Component
class AuthFilter(@Value("\${auth.token}") private val expectedToken: String) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.servletPath == "/actuator/health") {
            filterChain.doFilter(request, response)
            return
        }

        val authToken = request.getHeader("Authorization")
        if (authToken != expectedToken) {
            response.sendError(401)
            return
        }
        filterChain.doFilter(request, response)
    }
}