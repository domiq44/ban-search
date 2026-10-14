<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:if test="${param.lang eq 'en'}">
    <fmt:setLocale value="en" scope="request" />
</c:if>
<c:if test="${param.lang eq 'fr'}">
    <fmt:setLocale value="fr" scope="request" />
</c:if>
<fmt:setBundle basename="fr.exemple.adresses.messages" var="messages" />
<fmt:message key="page.language" bundle="${messages}" var="pageLanguage" />
<c:set var="languageTarget" value="${pageLanguage eq 'fr' ? 'en' : 'fr'}" />
<fmt:message
    key="${pageLanguage eq 'fr' ? 'language.switch.to.en' : 'language.switch.to.fr'}"
    bundle="${messages}"
    var="languageSwitchLabel" />
<fmt:message key="page.title" bundle="${messages}" var="pageTitle" />
<fmt:message key="page.description" bundle="${messages}" var="pageDescription" />
<fmt:message key="search.query.placeholder" bundle="${messages}" var="queryPlaceholder" />
<fmt:message key="search.query.label" bundle="${messages}" var="queryLabel" />
<fmt:message key="search.postcode.placeholder" bundle="${messages}" var="postcodePlaceholder" />
<fmt:message key="search.postcode.label" bundle="${messages}" var="postcodeLabel" />
<fmt:message key="search.type.label" bundle="${messages}" var="typeLabel" />
<fmt:message key="search.limit.label" bundle="${messages}" var="limitLabel" />
<fmt:message key="search.type.all" bundle="${messages}" var="typeAll" />
<fmt:message key="search.type.housenumber" bundle="${messages}" var="typeHousenumber" />
<fmt:message key="search.type.street" bundle="${messages}" var="typeStreet" />
<fmt:message key="search.type.locality" bundle="${messages}" var="typeLocality" />
<fmt:message key="search.type.municipality" bundle="${messages}" var="typeMunicipality" />
<fmt:message key="search.submit" bundle="${messages}" var="searchSubmit" />
<fmt:message key="status.initial" bundle="${messages}" var="statusInitial" />
<fmt:message key="status.searching" bundle="${messages}" var="statusSearching" />
<fmt:message key="status.empty" bundle="${messages}" var="statusEmpty" />
<fmt:message key="status.results.one" bundle="${messages}" var="statusResultOne" />
<fmt:message key="status.results.other" bundle="${messages}" var="statusResultOther" />
<fmt:message key="status.error.prefix" bundle="${messages}" var="statusErrorPrefix" />
<fmt:message key="results.address" bundle="${messages}" var="resultAddress" />
<fmt:message key="results.postcode" bundle="${messages}" var="resultPostcode" />
<fmt:message key="results.city" bundle="${messages}" var="resultCity" />
<fmt:message key="results.type" bundle="${messages}" var="resultType" />
<fmt:message key="results.score" bundle="${messages}" var="resultScore" />
<fmt:message key="results.coordinates" bundle="${messages}" var="resultCoordinates" />
<fmt:message key="results.open.map" bundle="${messages}" var="resultOpenMap" />
<c:set var="minimumQueryLength" value="3" />
<c:set var="postcodePattern" value="\d{5}" />
<!DOCTYPE html>
<html lang="<c:out value='${pageLanguage}' />">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${pageTitle}" /></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>

<body>
    <header>
        <a class="language-toggle" href="${pageContext.request.contextPath}/?lang=${languageTarget}"
            lang="${languageTarget}"
            aria-label="<c:out value='${languageSwitchLabel}' />"
            title="<c:out value='${languageSwitchLabel}' />">
            <img src="${pageContext.request.contextPath}/images/flag-${languageTarget}.svg" alt="">
        </a>
        <h1><c:out value="${pageTitle}" /></h1>
        <p><c:out value="${pageDescription}" /></p>
    </header>
    <main>
        <section class="card">
            <form id="form">
                <label class="field field-query">
                    <c:out value="${queryLabel}" />
                    <input name="q" placeholder="<c:out value='${queryPlaceholder}' />" required
                        minlength="<c:out value='${minimumQueryLength}' />" maxlength="200">
                </label>
                <label class="field field-postcode">
                    <c:out value="${postcodeLabel}" />
                    <input name="postcode" placeholder="<c:out value='${postcodePlaceholder}' />"
                        pattern="<c:out value='${postcodePattern}' />" maxlength="5" inputmode="numeric">
                </label>
                <label class="field field-type">
                    <c:out value="${typeLabel}" />
                    <select name="type">
                    <option value=""><c:out value="${typeAll}" /></option>
                    <option value="housenumber"><c:out value="${typeHousenumber}" /></option>
                    <option value="street"><c:out value="${typeStreet}" /></option>
                    <option value="locality"><c:out value="${typeLocality}" /></option>
                    <option value="municipality"><c:out value="${typeMunicipality}" /></option>
                    </select>
                </label>
                <label class="field field-limit">
                    <c:out value="${limitLabel}" />
                    <select name="limit">
                    <option>5</option>
                    <option>10</option>
                    <option>20</option>
                    </select>
                </label>
                <button type="submit"><c:out value="${searchSubmit}" /></button>
            </form>
        </section>
        <section class="card">
            <p id="msg" role="status" aria-live="polite" aria-atomic="true"
                data-searching="<c:out value='${statusSearching}' />"
                data-empty="<c:out value='${statusEmpty}' />"
                data-result-one="<c:out value='${statusResultOne}' />"
                data-result-other="<c:out value='${statusResultOther}' />"
                data-error-prefix="<c:out value='${statusErrorPrefix}' />"
                data-map-link-label="<c:out value='${resultOpenMap}' />">
                <c:out value="${statusInitial}" />
            </p>
            <div class="table-wrap">
                <table id="result" hidden>
                    <thead>
                        <tr>
                            <th><c:out value="${resultAddress}" /></th>
                            <th><c:out value="${resultPostcode}" /></th>
                            <th><c:out value="${resultCity}" /></th>
                            <th><c:out value="${resultType}" /></th>
                            <th><c:out value="${resultScore}" /></th>
                            <th><c:out value="${resultCoordinates}" /></th>
                        </tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </div>
        </section>
    </main>

    <script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>

</html>