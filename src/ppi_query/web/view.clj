(ns ppi-query.web.view
  (:use [hiccup core page form])
  (:require [ppi-query.organism :as orgn]
            [ppi-query.protein :as prot]
            [ppi-query.interaction.psicquic.registry :as reg]
            [ppi-query.network :as network]
            [ppi-query.graph   :as graph]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:import java.net.URI
           java.net.URLEncoder))

(defn view-layout [& content]
  (html
    (doctype :xhtml-strict)
    (xhtml-tag "en"
      [:head
        [:meta {:charset "utf-8"}]
        [:meta {:http-equiv "X-UA-Compatible"
                :content "IE=edge,chrome=1"}]
        [:meta {:name "description"
                :content "Fetch interactions from PSIQUICK from multiple orthologs, display them in VR"}]
        [:meta {:name "viewport"
                :content "width=device-width,initial-scale=1,maximum-scale=1,shrink-to-fit=no"}]
        [:title "Ppi-query"]
        (include-css "/css/normalize.css"
                     "/css/selectize.default.css")
        [:link {:rel "stylesheet"
                :href "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta.3/css/bootstrap.min.css"
                :integrity "sha384-Zug+QiDoJOrZ5t4lssLdxGhVrurbmBWopoEl+M6BdEfwnCJZtKxi1KgxUyJq13dy"
                :crossorigin "anonymous"}]
        (include-js "/js/jquery-3.2.1.min.js")
        [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js"
                  :integrity "sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q"
                  :crossorigin "anonymous"}]
        [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta.3/js/bootstrap.min.js"
                  :integrity "sha384-a5N7Y/aK3qNeh15eJKGWxsqtnX/wWdSZSKp+81YjTmS15nvnvxKHuzaWwXHDli+4"
                  :crossorigin "anonymous"}]
        (include-js "/js/selectize.min.js"
                    "https://aframe.io/releases/0.7.1/aframe.min.js"
                    ;"https://unpkg.com/aframe-forcegraph-component/dist/aframe-forcegraph-component.js")]
                    "/js/aframe-forcegraph-component.min.js"
                    "/js/aframe-gamepad-controls.min.js")]
     [:body content]
     [:script "$(function () {$('[data-toggle=\"tooltip\"]').tooltip()});"])))




(defn select-orgs [name & {mult :multiple sel :selected plac :placeholder :as args}]
  (let [selected
        (set (if (coll? sel) sel [sel]))]
    (into []
     (concat
      [:select {:name name :id name :multiple mult :placeholder plac}]
      (map (fn [{id :taxon-id com :common-name scient :scientific-name :as org}]
             (let [short (orgn/get-shortname org)]
               (vector
                 :option {:value id
                          :selected (or (contains? selected id)
                                        (contains? selected com)
                                        (contains? selected short)
                                        (contains? selected scient))}
                         (or scient short com))))
           (sort-by :scientific-name orgn/inparanoid-organism-repository))))))


(defn select-dbs [name & {mult :multiple sel :selected plac :placeholder :as args}]
  (let [selected
        (set (if (coll? sel) sel [sel]))]
    (into []
     (concat
      [:select {:name name :id name :multiple mult :placeholder plac}]
      (map (fn [db]
             (vector :option {:value db
                              :selected (contains? selected db)}
                             db))
           (sort (keys (reg/get-registry!))))))))

(defn view-input [dbs ref-org uniprotids oth-orgs]
  (view-layout
    [:br]
    [:div.container
      [:h2 "Query a protein"]
      (form-to [:get "/"]
        [:div.form-group
          (label "uniprotids" "Proteins (separated by `,` or `;`): ")
          [:input#uniprotids.form-control {:type "text" :name "uniprotids" :value (str/join ", " uniprotids)}]]
        [:div.form-group
          (label "ref-org" "Reference organism: ")
          (select-orgs "ref-org" :selected ref-org :placeholder "Choose a reference organism...")]
        [:script "$('#ref-org').selectize();"]
        [:div.form-group
          (label "oth-orgs" "Other organisms: ")
          (select-orgs "oth-orgs" :multiple true :selected oth-orgs :placeholder "Choose some orthologs...")]
        [:script "$('#oth-orgs').selectize();"]
        [:div.form-group
          (label "dbs" "Databases: ")
          (select-dbs "dbs" :multiple true :selected dbs :placeholder "Choose some databases...")]
        [:script "$('#dbs').selectize();"]
        [:div.row.justify-content-around
          [:button.btn.btn-primary.col-4 {:type "submit"} "Fetch network"]]
        [:br]
        [:div.row.justify-content-around
          [:a.btn.btn-secondary.col-2 {:href "/?dbs=IntAct&ref-org=6239&uniprotids=Q20646%2CQ18688&oth-orgs=10090&oth-orgs=284812&oth-orgs=3702"}
              "C.elegans + 3 orgs"]
          [:a.btn.btn-secondary.col-2 {:href "/?dbs=IntAct&ref-org=6239&uniprotids=Q20646%2CQ18688&oth-orgs=10090&oth-orgs=284812&oth-orgs=3702&oth-orgs=9913&oth-orgs=7227&oth-orgs=10116"}
              "C.elegans + 6 orgs"]
          [:a.btn.btn-secondary.col-2 {:href "/?dbs=IntAct&ref-org=6239&uniprotids=Q20646%2CQ18688&oth-orgs=10090&oth-orgs=284812&oth-orgs=3702&oth-orgs=9913&oth-orgs=7227&oth-orgs=10116&oth-orgs=9606&oth-orgs=S.cerevisiae"}
              "C.elegans + 8 orgs"]
          [:a.btn.btn-secondary.col-2 {:href "/?dbs=IntAct&ref-org=9606&uniprotids=P08238&oth-orgs=10090&oth-orgs=10090&oth-orgs=284812&oth-orgs=3702&oth-orgs=9913&oth-orgs=7227&oth-orgs=10116&oth-orgs=9606&oth-orgs=S.cerevisiae2"}
              "Human + 8 orgs"]])]))

(defn html-mutlilines [args]
  (into []
    (concat [:div]
      (map #(vector :p %)
           args))))

(def ^:dynamic *encoding* "UTF-8")

(defmacro with-encoding
  "Sets a default encoding for URL encoding strings. Defaults to UTF-8."
  [encoding & body]
  `(binding [*encoding* ~encoding]
     ~@body))

(defprotocol URLEncode
  (url-encode [x] "Turn a value into a URL-encoded string."))

(extend-protocol URLEncode
  String
  (url-encode [s] (URLEncoder/encode s *encoding*))
  java.util.Map
  (url-encode [m]
    (str/join "&"
      (for [[k v] m]
        (if (vector? v)
          (str/join "&"
            (map #(str (url-encode k) "=" (url-encode %))
                 v))
          (str (url-encode k) "=" (url-encode v)))))))

(defn href-back [dbs ref-org uniprotids oth-orgs]
  (str "/?"
    (url-encode
      {"dbs" (vec dbs)
       "ref-org" (str ref-org)
       "uniprotids" (str/join "," uniprotids)
       "oth-orgs" (vec (map str oth-orgs))
       "force-input" "true"})))
(defn href-back-objs [dbs ref-org prots oth-orgs]
  (str "/?"
    (url-encode
      {"dbs" (vec dbs)
       "ref-org" (str (:taxon-id ref-org))
       "uniprotids" (str/join "," (map :uniprotid prots))
       "oth-orgs" (vec (map (comp str :taxon-id) oth-orgs))
       "force-input" "true"})))

(defn view-output-html [dbs ref-org uniprotids oth-orgs]
  (view-layout
    [:br]
    [:div.container
      [:h2 "Fetching the network for:"]
      [:div.row
        [:div.col
          [:h3 "Uniprot Ids:"]
          (html-mutlilines uniprotids)]
        [:div.col
          [:h3 "Reference organism:"]
          [:p ref-org]]]
      [:div.row
        [:div.col
          [:h3 "Databases:"]
          (html-mutlilines dbs)]
        [:div.col
          [:h3 "Other orthologs:"]
          (html-mutlilines oth-orgs)]]
      [:div.row.justify-content-center
        [:a.col-8.btn.btn-primary {:href (href-back dbs ref-org uniprotids oth-orgs)}
            "Get another network"]]]))

(defn view-graph-json [nodes-edges]
  [:a-scene ;{:stats ""}
    [:a-camera {:wasd-controls "fly: true; acceleration: 300"
                :gamepad-controls__0 "controller:0;flyEnabled: true; acceleration: 300;"
                :gamepad-controls__1 "controller:1;flyEnabled: true; acceleration: 300;"
                :gamepad-controls__2 "controller:2;flyEnabled: true; acceleration: 300;"
                :gamepad-controls__3 "controller:3;flyEnabled: true; acceleration: 300;"}
      [:a-cursor {:color "lavender" :opacity "0.5"}]]
    [:a-sky {:color "#002"}]

    [:a-entity {:forcegraph
                (str "nodes:" (json/write-str (nodes-edges :nodes))
                     ";links:" (json/write-str (nodes-edges :links))
                     ";node-id: id;node-label: label;"
                     "link-source:from;link-target:to;"
                     "link-label:label;link-desc:desc;"
                     "link-auto-color-by:desc;")}]])

(defn or-common-scientific [org]
  (or (:common-name org) (orgn/get-shortname org) (:scientific-name org)))

(defn span-org [org]
  [:span.badge.badge-info
    {:data-toggle "tooltip" :data-placement "bottom"
     :title (str (:scientific-name org)
                 (if-let [com (:common-name org)] (str " (Common name: " com ")"))
                 " (Taxon: " (:taxon-id org) ")")}
    (orgn/get-shortname org)])

(defn span-prot [uniprotid]
  [:span.badge.badge-primary uniprotid])

(defn span-db [db]
  [:span.badge.badge-success db])

(defn view-output-graph [dbs ref-org uniprotids oth-orgs]
  (let [ref-organism
          (orgn/inparanoid-organism-by-id-or-shortname ref-org)
        proteins
          (map (partial prot/->Protein ref-organism)
               uniprotids)
        other-organisms
          (map orgn/inparanoid-organism-by-id-or-shortname oth-orgs)
        [ret-proteins ret-interactions]
        (network/fetch-protein-network
          dbs ref-organism proteins other-organisms)
        nodes-edges (graph/to-graph-data ret-proteins ret-interactions)]
    (view-layout
      [:nav.navbar.navbar-expand-lg.navbar-light.bg-light
        [:a.navbar-brand {:href "/"} "Ppi-Query"]
        [:button.navbar-toggler
          {:type "button"
           :data-toggle "collapse" :data-target "#navbarText"
           :aria-controls "navbarText" :aria-expanded "false"
           :aria-label "Toggle navigation"}
          [:span.navbar-toggler-icon ""]]
        [:div#navbarText.collapse.navbar-collapse
          [:span.navbar-text
            {:style "margin-right:8px;"}
            "Graph generated for "
            (into [:span] (interpose ", " (map span-prot uniprotids)))
            " (" (span-org ref-organism) ") "
            "with orthologs: "
            (into [:span] (interpose " " (map span-org other-organisms)))
            ". DBs: "
            (into [:span] (interpose ", " (map span-db dbs)))]
          [:form.form-inline
            [:a.btn.btn-outline-danger {:href (href-back-objs dbs ref-organism proteins other-organisms)}
               "Back"]]]]
      (view-graph-json nodes-edges))))

(def view-output view-output-graph)
