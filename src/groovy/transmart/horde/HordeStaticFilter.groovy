package transmart.horde

import org.springframework.web.filter.GenericFilterBean

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

class HordeStaticFilter extends GenericFilterBean {

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        System.err.println("HOUAAAAAHOUUUUUUU")
        invokeDelegate(this.delegate, request, response, filterChain);
//        filterChain.doFilter(request, response);
    }

    protected void invokeDelegate(Filter delegate, ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        delegate.doFilter(request, response, filterChain);
    }
}
