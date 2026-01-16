(function() {
    if (window.Bridge) return;

    const __bridgeInternal = window.__bridgeInternal;
    delete window.__bridgeInternal; // Hide internal function

    let objectRegistry = {};

    const Bridge = {
        __internal: {
            async sendMessageToJava(type, data) {
                return await __bridgeInternal(type, data);
            },

            defineObject(path, id) {
                const parts = path.split('.');
                const propertyName = parts.pop();

                let proxy;
                const propertyCache = {}; // Cache for property values

                const object = {
                    __internal: {
                        id: id,
                        propertyCache: propertyCache,

                        defineFunction(name) {
                            object[name] = async function() {
                                return await Bridge.__internal.invoke(
                                    id,
                                    name,
                                    Array.from(arguments)
                                );
                            };
                        },

                        defineProperty(name) {
                            // Initialize property in cache
                            propertyCache[name] = undefined;
                            
                            Object.defineProperty(object, name, {
                                get() {
                                    // Trigger async fetch to update cache
                                    Bridge.__internal.get(id, name).then(value => {
                                        propertyCache[name] = value;
                                    });
                                    // Return current cached value immediately
                                    return propertyCache[name];
                                },
                                set(value) {
                                    propertyCache[name] = value;
                                    Bridge.__internal.set(id, name, value);
                                },
                                enumerable: true,
                                configurable: true
                            });
                            
                            // Fetch initial value
                            Bridge.__internal.get(id, name).then(value => {
                                propertyCache[name] = value;
                            });
                        }
                    }
                };

                const handler = {
                    get(obj, property) {
                        // If property exists on object, return it
                        if (property in obj) {
                            return obj[property];
                        }
                        
                        // For dynamic properties, trigger fetch and return cached value
                        Bridge.__internal.get(id, property).then(value => {
                            propertyCache[property] = value;
                        });
                        
                        return propertyCache[property];
                    },
                    set(obj, property, value) {
                        propertyCache[property] = value;
                        Bridge.__internal.set(id, property, value);
                        return true;
                    }
                };

                Object.freeze(object.__internal);
                proxy = new Proxy(object, handler);

                // Resolve the root object
                let root = window;
                for (const part of parts) {
                    root = root[part];
                }

                Object.defineProperty(root, propertyName, {
                    value: proxy,
                    writable: true,
                    configurable: true
                });

                objectRegistry[id] = proxy;
            },

            async get(id, property) {
                return await Bridge.__internal.sendMessageToJava('GET', { id, property });
            },

            async set(id, property, newValue) {
                return await Bridge.__internal.sendMessageToJava('SET', {
                    id,
                    property,
                    newValue
                });
            },

            async invoke(id, func, arguments) {
                return await Bridge.__internal.sendMessageToJava('INVOKE', {
                    id,
                    function: func,
                    arguments
                });
            }
        }
    };

    Object.freeze(Bridge);
    Object.freeze(Bridge.__internal);
    Object.defineProperty(window, 'Bridge', {
        value: Bridge,
        writable: false,
        configurable: false
    });

    console.log('[Bridge] Initialized');
})();
